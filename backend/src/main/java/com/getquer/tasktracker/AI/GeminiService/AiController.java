package com.getquer.tasktracker.AI.GeminiService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final GeminiService geminiService;

    public AiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/analyze-task")
    public ResponseEntity<String> analyzeTask(
            @RequestBody String description
    ){
        String result = geminiService.generateSubTask(description);
        return ResponseEntity.ok(result);
    }
}