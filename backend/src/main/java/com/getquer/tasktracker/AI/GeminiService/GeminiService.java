package com.getquer.tasktracker.AI.GeminiService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {
    private final ChatClient chatClient;

    // API ключ и название модели подтянутся из application.properties
    public GeminiService(ChatClient.Builder chatClientBuilder){
        this.chatClient = chatClientBuilder.build();
    }

    public String generateSubTask(String taskDescription){
        String systemPromt = "Ты AI-ассистент в Task Tracker. Разбей следующую задачу на несколько конкретных подзадач в формате списка.";

        return chatClient.prompt()
                .system(systemPromt)
                .user("Задача: " + taskDescription )
                .call()
                .content();
    }
}