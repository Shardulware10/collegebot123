package com.college.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** Calls Google Gemini API (cloud AI service). Key comes from env var GEMINI_API_KEY. */
@Service
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM =
        "You are CollegeBot, a friendly assistant for college students. "
        + "Answer ANY question the student asks (studies, coding, cloud computing, general knowledge, career advice). "
        + "ALWAYS reply in clear, simple English, even if the student writes in another language, "
        + "unless they explicitly ask for a different language. "
        + "Keep answers short (max 6-7 lines) unless the student asks for detail. "
        + "For college-specific details like exact timetable, fees or exam dates, say the student should check the notice board "
        + "or contact office@college.edu, because you do not have live college data.";

    private volatile String lastError = "none";

    public String lastError() {
        return lastError;
    }

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey = System.getenv("GEMINI_API_KEY");
    private final String model = envOr("GEMINI_MODEL", "gemini-3.8-flash");

    private static String envOr(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    public Optional<String> ask(List<ChatMessage> history, String message) {
        if (!enabled()) return Optional.empty();
        try {
            ArrayNode contents = MAPPER.createArrayNode();
            boolean started = false;
            for (ChatMessage m : history) {
                boolean isUser = "user".equals(m.getSender());
                if (!started && !isUser) continue; // must start with a user turn
                started = true;
                ObjectNode c = contents.addObject();
                c.put("role", isUser ? "user" : "model");
                c.putArray("parts").addObject().put("text", m.getMessage());
            }
            ObjectNode cur = contents.addObject();
            cur.put("role", "user");
            cur.putArray("parts").addObject().put("text", message);

            ObjectNode body = MAPPER.createObjectNode();
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM);
            body.set("contents", contents);
            body.putObject("generationConfig").put("maxOutputTokens", 2000);

            HttpRequest req = HttpRequest.newBuilder(URI.create(
                    "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent"))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) {
                String b = res.body();
                log.warn("Gemini API error {}: {}", res.statusCode(), b.substring(0, Math.min(300, b.length())));
                lastError = "HTTP " + res.statusCode() + (res.statusCode() == 429 ? " (free limit reached)"
                        : res.statusCode() == 404 ? " (wrong model name)"
                        : (res.statusCode() == 400 || res.statusCode() == 403) ? " (wrong API key or model setting)" : "");
                return Optional.empty();
            }
            JsonNode parts = MAPPER.readTree(res.body()).path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) sb.append(p.path("text").asText(""));
            String text = sb.toString().trim();
            if (text.isEmpty()) { lastError = "empty reply from AI"; return Optional.empty(); }
            lastError = "none";
            return Optional.of(text);
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.toString());
            lastError = "connection problem: " + e.getClass().getSimpleName();
            return Optional.empty();
        }
    }
}
