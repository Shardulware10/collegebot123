package com.college.chatbot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatService {

    record Intent(String name, List<String> keys, String reply) {}

    public record Result(String intent, String reply) {}

    private static final List<Intent> INTENTS = List.of(
        new Intent("greeting", List.of("hi", "hello", "hey", "namaskar", "namaste"),
            "Hello! 👋 I'm CollegeBot. Ask me anything, or try timetable, exam, fees, library, placement, hostel or contact."),
        new Intent("timetable", List.of("timetable", "time table", "lecture", "schedule", "vel", "lectures"),
            "📅 Lectures run from 9 AM to 4 PM every day. The full timetable is in the notice section."),
        new Intent("exam", List.of("exam", "exams", "test", "paper", "pariksha", "result", "dates"),
            "📝 Exams are held at the end of each semester. Exact dates are posted on the notice board."),
        new Intent("fees", List.of("fees", "fee", "payment", "paise", "scholarship"),
            "💰 Fees are paid at the accounts office. Check the notice board for the last date."),
        new Intent("library", List.of("library", "book", "books", "pustak"),
            "📚 The library is open 9 AM to 5 PM. Bring your ID card."),
        new Intent("placement", List.of("placement", "job", "jobs", "company", "internship"),
            "💼 Register through the Placement Cell notices. Internship details are posted there too."),
        new Intent("hostel", List.of("hostel", "mess", "room", "canteen"),
            "🏠 Contact the hostel warden for hostel and mess details. The canteen is open 8 AM to 6 PM."),
        new Intent("contact", List.of("contact", "phone", "email", "office", "number"),
            "📞 Email: office@college.edu | Phone: 020-12345678"),
        new Intent("bye", List.of("bye", "thanks", "thank", "dhanyavad"),
            "Thank you! 😊 See you again.")
    );

    private static final String FALLBACK =
        "Sorry, I didn't understand that 😕 Try timetable, exam, fees, library, placement, hostel or contact.";

    private final ChatRepository repo;
    private final AiClient ai;

    public ChatService(ChatRepository repo, AiClient ai) {
        this.repo = repo;
        this.ai = ai;
    }

    public Result answer(String text) {
        String t = text.toLowerCase();
        String[] tokens = t.split("[^a-z0-9]+");
        Intent best = null;
        int bestScore = 0;
        for (Intent in : INTENTS) {
            int score = 0;
            for (String key : in.keys()) {
                if (key.contains(" ")) {
                    if (t.contains(key)) score += 2;
                } else {
                    for (String tok : tokens) {
                        if (tok.equals(key)) { score += 2; break; }
                        if (key.length() >= 5 && tok.length() >= 4 && distance(tok, key) <= 1) { score += 1; break; }
                    }
                }
            }
            if (score > bestScore) { bestScore = score; best = in; }
        }
        return best == null ? new Result("unknown", FALLBACK) : new Result(best.name(), best.reply());
    }

    public Result chat(String sessionId, String message) {
        List<ChatMessage> all = repo.findBySessionIdOrderByIdAsc(sessionId);
        List<ChatMessage> recent = all.subList(Math.max(0, all.size() - 10), all.size());
        Optional<String> aiReply = ai.ask(recent, message);
        Result r = aiReply.map(t -> new Result("ai", t)).orElseGet(() -> {
            Result fb = answer(message);
            if (!fb.intent().equals("unknown")) return fb;
            if (ai.enabled()) {
                return new Result("error", "⚠️ Can't reach the AI right now (" + ai.lastError() + "). Please try again in a moment.");
            }
            return new Result("unknown", "AI mode is OFF. Set GEMINI_API_KEY on the server so I can answer any question 🙂");
        });
        repo.save(new ChatMessage(sessionId, "user", message, null));
        repo.save(new ChatMessage(sessionId, "bot", r.reply(), r.intent()));
        return r;
    }

    public boolean aiEnabled() { return ai.enabled(); }
    public String aiModel() { return ai.model(); }
    public String aiError() { return ai.lastError(); }

    public List<ChatMessage> history(String sessionId) {
        return repo.findBySessionIdOrderByIdAsc(sessionId);
    }

    @Transactional
    public void clear(String sessionId) {
        repo.deleteBySessionId(sessionId);
    }

    public Map<String, Object> stats() {
        List<Map<String, Object>> top = new ArrayList<>();
        for (Object[] row : repo.intentCounts()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("intent", row[0]);
            m.put("count", row[1]);
            top.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalMessages", repo.count());
        out.put("userQuestions", repo.countBySender("user"));
        out.put("sessions", repo.countSessions());
        out.put("intents", top);
        return out;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }
}
