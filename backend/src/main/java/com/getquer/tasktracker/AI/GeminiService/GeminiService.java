package com.getquer.tasktracker.AI.GeminiService;

// Временно закомментировано для сборки проекта - Spring AI зависимости отключены
// import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {
    // private final ChatClient chatClient;

    // Временно закомментировано для сборки проекта
    /*
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
    */

    public String generateSubTask(String taskDescription){
        return "AI временно отключен. Spring AI зависимости будут добавлены позже.";
    }
}