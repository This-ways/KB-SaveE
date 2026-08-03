package org.scoula.report.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// OpenAI Chat Completions API 응답 body 중, 우리가 실제로 쓰는 부분만 옮겨 담음
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenAiChatResponse {
    private List<Choice> choices;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private OpenAiChatRequest.Message message;
    }
}
