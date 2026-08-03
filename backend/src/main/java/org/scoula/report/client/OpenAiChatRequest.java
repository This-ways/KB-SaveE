package org.scoula.report.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// OpenAI Chat Completions API 요청 body 모양 그대로 맞춘 DTO
// https://platform.openai.com/docs/api-reference/chat/create
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatRequest {
    private String model;
    private List<Message> messages;
    private Integer max_tokens; // OpenAI가 snake_case를 쓰므로 필드명도 그대로 맞춤

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;    // "system" 또는 "user"
        private String content;
    }
}
