package br.com.adautomoises;

import com.google.auth.oauth2.GoogleCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {
    public static void main(String[] args) {
        try (HttpClient httpClient = HttpClient.newBuilder().build()) {
            String jsonRequestBody = """
                    {
                        "contents": [
                            {
                                "role": "user",
                                "parts": [
                                    {
                                        "text": "Como executar um comando utilizado vertex ai api?"
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
                        "generationConfig": {
                            "candidateCount": 1,
                            "temperature": 0.2,
                            "maxOutputTokens": 1024,
                            "topP": 0.8,
                            "topK": 40
                        },
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
                    """;

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

    private static String getAccessToken() throws IOException{
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