package com.college.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);

    void deleteBySessionId(String sessionId);

    long countBySender(String sender);

    @Query("select count(distinct m.sessionId) from ChatMessage m")
    long countSessions();

    @Query("select m.intent, count(m) from ChatMessage m where m.sender = 'bot' group by m.intent order by count(m) desc")
    List<Object[]> intentCounts();
}
