package org.scoula.report.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.peerstat.dto.CategoryCompareDTO;
import org.scoula.report.client.OpenAiChatRequest;
import org.scoula.report.client.OpenAiChatResponse;
import org.scoula.report.domain.AiSummaryVO;
import org.scoula.report.mapper.AiSummaryMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 150;

    // OpenAI 호출 실패(키 문제, 네트워크 오류, 한도 초과 등) 시 사용자에게 보여줄 대체 문구
    private static final String FALLBACK_SUMMARY =
            "이번 달 소비 데이터를 확인해보세요! 카테고리별 지출을 살펴보면 절약 포인트를 찾을 수 있어요.";

    private final RestTemplate restTemplate;
    private final AiSummaryMapper aiSummaryMapper;

    // 팀 공지 규칙: 코드에 키를 직접 적지 않고 환경변수 OPENAI_API_KEY에서만 읽음
    private final String apiKey = System.getenv("OPENAI_API_KEY");

    @Override
    public String getOrGenerateSummary(Long userId, String yearMonth, int income, int expense,
                                        List<CategoryCompareDTO> topCategories) {
        // 1. 캐시 확인 - 이미 이 달 요약을 만든 적 있으면 OpenAI 호출 없이 그대로 반환
        AiSummaryVO cached = aiSummaryMapper.find(userId, yearMonth);
        if (cached != null) {
            log.debug("AI 요약 캐시 히트 userId={} yearMonth={}", userId, yearMonth);
            return cached.getSummaryText();
        }

        // 2. 캐시 없으면 새로 생성
        String summary;
        try {
            summary = callOpenAi(buildPrompt(income, expense, topCategories));
        } catch (Exception e) {
            log.error("OpenAI 호출 실패, 폴백 문구로 대체합니다. userId={} yearMonth={}", userId, yearMonth, e);
            return FALLBACK_SUMMARY; // 실패 시 DB에 저장하지 않음 -> 다음 요청 때 다시 시도됨
        }

        // 3. 성공하면 캐시에 저장 (다음부터는 DB에서 바로 꺼내 씀)
        aiSummaryMapper.insert(new AiSummaryVO(null, userId, yearMonth, summary, null));

        return summary;
    }

    private String buildPrompt(int income, int expense, List<CategoryCompareDTO> topCategories) {
        StringBuilder sb = new StringBuilder();
        sb.append("사용자의 이번 달 소비 데이터를 보고, 2~3문장으로 친근하게 소비 습관 피드백을 해줘.\n");
        sb.append("이번 달 수입: ").append(income).append("원\n");
        sb.append("이번 달 지출: ").append(expense).append("원\n");
        sb.append("주요 소비 카테고리 (내 소비 vs 또래 평균):\n");
        for (CategoryCompareDTO c : topCategories) {
            String peer = (c.getPeerAmount() != null) ? c.getPeerAmount() + "원" : "정보없음";
            sb.append("- ").append(c.getCategoryName())
                    .append(": 나 ").append(c.getMyAmount()).append("원, 또래 평균 ").append(peer).append("\n");
        }
        sb.append("존댓말로, 너무 길지 않게, 격려하는 톤으로 작성해줘.");
        return sb.toString();
    }

    private String callOpenAi(String prompt) {
        List<OpenAiChatRequest.Message> messages = List.of(
                new OpenAiChatRequest.Message("system", "너는 친절한 가계부 앱의 소비 분석 도우미야."),
                new OpenAiChatRequest.Message("user", prompt)
        );
        OpenAiChatRequest request = new OpenAiChatRequest(MODEL, messages, MAX_TOKENS);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<OpenAiChatRequest> entity = new HttpEntity<>(request, headers);

        OpenAiChatResponse response = restTemplate.postForObject(OPENAI_URL, entity, OpenAiChatResponse.class);

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어있습니다.");
        }
        return response.getChoices().get(0).getMessage().getContent().trim();
    }
}
