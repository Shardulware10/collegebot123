package com.college.chatbot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    public record ChatRequest(String sessionId, String message) {}

    private final ChatService service;

    public ChatController(ChatService service) {
        this.service = service;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest req) {
        if (req.sessionId() == null || req.sessionId().isBlank()
                || req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId and message are required"));
        }
        String msg = req.message().trim();
        if (msg.length() > 300) msg = msg.substring(0, 300);
        ChatService.Result r = service.chat(req.sessionId(), msg);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("reply", r.reply());
        out.put("intent", r.intent());
        out.put("time", Instant.now().toString());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/history")
    public List<ChatMessage> history(@RequestParam String sessionId) {
        return service.history(sessionId);
    }

    @DeleteMapping("/history")
    public Map<String, String> clear(@RequestParam String sessionId) {
        service.clear(sessionId);
        return Map.of("status", "cleared");
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", "UP");
        out.put("serverTime", Instant.now().toString());
        out.put("java", System.getProperty("java.version"));
        out.put("aiError", service.aiError());
        out.put("ai", service.aiEnabled() ? "ON (" + service.aiModel() + ")" : "OFF (FAQ mode)");
        try {
            out.put("host", InetAddress.getLocalHost().getHostName());
        } catch (Exception e) {
            out.put("host", "unknown");
        }
        return out;
    }
}
