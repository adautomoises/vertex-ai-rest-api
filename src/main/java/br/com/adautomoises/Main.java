package br.com.adautomoises;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            String jsonRequestBody = String.format("""
                    {
                        "contents": [
                            {
                                "role": "user",
                                "parts": [
                                    {
                                        "text": "%s"
                                    }
                                ]
                            }
                        ]
                        , "systemInstruction": {
                            "parts": [
                            {
                                "text": "Você é um especialista em Vertex AI e deve auxiliar a contextualizar e desenvolver prompts excelentes."
                            }
                          ]
                        },
                        "generationConfig": %s,
                        "safetySettings": [
                            {
                                "category": "HARM_CATEGORY_HATE_SPEECH",
                                "threshold": "OFF"
                            },
                            {
                                "category": "HARM_CATEGORY_DANGEROUS_CONTENT",
                                "threshold": "OFF"
                            },
                            {
                                "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                                "threshold": "OFF"
                            },
                            {
                                "category": "HARM_CATEGORY_HARASSMENT",
                                "threshold": "OFF"
                            }
                        ]
                    }
                    """, getContentValue(), getParameters());

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(
                            String.format("https://%s/v1/projects/%s/locations/%s/publishers/google/models/%s:%s",
                                    System.getenv("API_ENDPOINT"), System.getenv("PROJECT_ID"),
                                    System.getenv("LOCATION_ID"), System.getenv("MODEL_ID"),
                                    System.getenv("GENERATE_CONTENT_API"))
                    )).header("Content-Type", "application/json")
                    .header("Authorization", String.format("Bearer %s", getAccessToken()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response -> \n" + httpResponse.body());

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static String getContentValue() throws IOException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(Main.class.getClassLoader()
                .getResourceAsStream("exemplos.json"))) {
            Gson gson = new Gson();
            Map<String, Object>[] exemplos = gson.fromJson(inputStreamReader, TypeToken.of(Map[].class).getType());

            StringBuilder stringBuilder = new StringBuilder();
            for (Map<String, Object> exemplo : exemplos) {
                String inputValue = (String) exemplo.get("input");
                String outputValue = (String) exemplo.get("output");

                stringBuilder.append(
                        String.format("Input: %s\nOutput: %s\n\n", inputValue, outputValue)
                );
            }

            return String.format("%s\n\ninput: %s\noutput:\n", System.getenv("CONTEXT"), inputStreamReader, getPrompt());
        }
    }

    private static String getPrompt() {
        System.out.println("Digite um comando:");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    private static String getParameters() {
        Map<String, Object> parametros = new HashMap<>();

        parametros.put("candidateCount", Integer.parseInt(System.getenv("CANDIDATE_COUNT")));
        parametros.put("maxOutputTokens", Integer.parseInt(System.getenv("MAX_OUTPUT_TOKENS")));
        parametros.put("temperature", Double.parseDouble(System.getenv("TEMPERATURE")));
        parametros.put("topP", Double.parseDouble(System.getenv("TOP-P")));
        parametros.put("topK", Integer.parseInt(System.getenv("TOP-K")));

        return new Gson().toJson(parametros);
    }

    private static String getAccessToken() throws IOException {
        try (InputStream serviceAccountStream = Main.class.getClassLoader()
                .getResourceAsStream(System.getenv("SERVICE_ACCOUNT_FILE_PATH"))) {

            assert serviceAccountStream != null;
            GoogleCredentials googleCredentials = GoogleCredentials
                    .fromStream(serviceAccountStream)
                    .createScoped(System.getenv("SCOPES"));

            googleCredentials.refreshIfExpired();

            return googleCredentials.getAccessToken().getTokenValue();
        }
    }
}