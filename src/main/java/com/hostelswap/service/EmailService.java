package com.hostelswap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Sends emails through Brevo's HTTPS API.
// Render's free plan blocks normal SMTP email ports, so the app sends mail over HTTPS instead.
// Settings come from environment variables, never from files in the project:
//   BREVO_API_KEY        - the API key from Brevo (SMTP & API > API Keys)
//   HOSTELSWAP_MAILFROM  - the sender address verified in Brevo
@Service
public class EmailService {
    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String from;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public EmailService(@Value("${BREVO_API_KEY:}") String apiKey,
                        @Value("${hostelswap.mail-from:}") String from) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.from = from == null ? "" : from.trim();
    }

    // Sends a plain-text email. Throws IllegalStateException with a readable message if it fails.
    public void send(String to, String subject, String text) {
        if (apiKey.isEmpty() || from.isEmpty()) {
            throw new IllegalStateException("Email sending is not set up. Set BREVO_API_KEY and HOSTELSWAP_MAILFROM.");
        }
        String json = "{"
                + "\"sender\":{\"name\":\"HostelSwap\",\"email\":" + quote(from) + "},"
                + "\"to\":[{\"email\":" + quote(to) + "}],"
                + "\"subject\":" + quote(subject) + ","
                + "\"textContent\":" + quote(text)
                + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(BREVO_URL))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.err.println("Brevo error " + resp.statusCode() + ": " + resp.body());
                throw new IllegalStateException("Could not send the email. Please try again in a minute.");
            }
        } catch (java.io.IOException e) {
            System.err.println("Brevo request failed: " + e.getMessage());
            throw new IllegalStateException("Could not send the email. Please try again in a minute.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Could not send the email. Please try again in a minute.");
        }
    }

    // Makes a safe JSON string value
    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }
}
