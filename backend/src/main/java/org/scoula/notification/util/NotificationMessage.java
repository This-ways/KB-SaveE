package org.scoula.notification.util;

import java.util.HashMap;
import java.util.Map;

// 알림 문구 모음
// 문구를 DB에 저장하지 않는 구조라 발송/조회 양쪽에서 여기를 참조함
// 카테고리명은 팀 DDL 기준 (식비, 카페/간식, 온라인쇼핑, 패션/쇼핑, 문화/여가,
//                        술/유흥, 교통, 생활, 뷰티, 교육, 반려동물, 여행, 기타)
public class NotificationMessage {

    // 푸시 제목 - "식비 예산 50% 소진" 형태로 조립
    private static final Map<Long, String> CATEGORY_NAMES = new HashMap<>();

    // 예산 알림 본문 - key: "카테고리ID:임계값" (예: "1:50")
    private static final Map<String, String> MESSAGES = new HashMap<>();

    // 적금 알림 제목/본문 - key: type_code (PAY_UPCOMING/PAY_SUCCESS/PAY_FAIL)
    private static final Map<String, String> PAY_TITLE_MAP = new HashMap<>();
    private static final Map<String, String> PAY_BODY_MAP = new HashMap<>();

    static {
        CATEGORY_NAMES.put(1L, "식비");
        CATEGORY_NAMES.put(2L, "카페/간식");
        CATEGORY_NAMES.put(3L, "온라인쇼핑");
        CATEGORY_NAMES.put(4L, "패션/쇼핑");
        CATEGORY_NAMES.put(5L, "문화/여가");
        CATEGORY_NAMES.put(6L, "술/유흥");
        CATEGORY_NAMES.put(7L, "교통");
        CATEGORY_NAMES.put(8L, "생활");
        CATEGORY_NAMES.put(9L, "뷰티");
        CATEGORY_NAMES.put(10L, "교육");
        CATEGORY_NAMES.put(11L, "반려동물");
        CATEGORY_NAMES.put(12L, "여행");
        CATEGORY_NAMES.put(13L, "기타");

        // 1. 식비
        MESSAGES.put("1:50", "식비 예산 딱 절반 달성! 남은 기간은 집밥 비율을 좀 늘려볼까요?");
        MESSAGES.put("1:70", "이 페이스면 남은 날은 하루 한 끼만 맛집 찾아가야 해요");
        MESSAGES.put("1:90", "식비 예산 멸종 직전! 오늘부터 냉장고 파먹기 대작전 시작!");

        // 2. 카페/간식
        MESSAGES.put("2:50", "카페 예산 절반 돌파! 디카페인이나 텀블러 할인 챙길 타임");
        MESSAGES.put("2:70", "아메리카노 지수가 위험해요! 하루 한 잔으로 조율해 볼까요?");
        MESSAGES.put("2:90", "간식 예산 비상! 당분간 회사/집에 있는 무료 탕비실을 이용해 주세요");

        // 3. 온라인쇼핑
        MESSAGES.put("3:50", "장바구니 결제 절반 달성! 꼭 필요한 물건인지 한번 더 고민해 보세요");
        MESSAGES.put("3:70", "택배 상자가 쌓이고 있어요! 찜 목록을 잠시 봉인할 시간");
        MESSAGES.put("3:90", "쇼핑 예산 임계점! '지금 구매하기' 버튼에서 손을 떼야 합니다");

        // 4. 패션/쇼핑
        MESSAGES.put("4:50", "옷장 예산 50% 사용! 이번 달 신상은 여기까지만 둘러볼까요?");
        MESSAGES.put("4:70", "이번 달 패션 지수 완충 직전! 있는 옷으로 돌려입기 스킬 발동!");
        MESSAGES.put("4:90", "옷장 예산 초비상! 쇼핑 앱 알림을 잠시 꺼두는 걸 추천해요");

        // 5. 문화/여가
        MESSAGES.put("5:50", "문화생활 예산 반토막! 다음 예매는 할인 혜택부터 체크하세요");
        MESSAGES.put("5:70", "이번 달 여가 예산 위험! 무료 전시나 산책으로 힐링해 볼까요?");
        MESSAGES.put("5:90", "여가 예산 바닥이에요! 쓰지 않는 OTT 구독부터 일시 정지해 볼까요?");

        // 6. 술/유흥
        MESSAGES.put("6:50", "모임/술자리 예산 절반 소진! 간 건강과 통장 건강을 동시에 챙깁니다");
        MESSAGES.put("6:70", "이번 달 2차/3차는 위험해요! 귀가 시간을 조금 당겨볼까요?");
        MESSAGES.put("6:90", "유흥 예산 빨간불! 오늘부터 약속은 '다음 달에 만나요'로 연기!");

        // 7. 교통
        MESSAGES.put("7:50", "교통비 예산 절반 소진! 환승 할인 꼼꼼히 챙기고 계시죠?");
        MESSAGES.put("7:70", "택시 호출 앱 접속 금지! 남은 기간은 대중교통 이용이 안전해요");
        MESSAGES.put("7:90", "교통비 위험 경보! 가까운 거리는 건강을 위해 걸어가 볼까요?");

        // 8. 생활
        MESSAGES.put("8:50", "생활필수품 예산 50% 사용! 대용량 할인이나 묶음 상품을 활용해 보세요");
        MESSAGES.put("8:70", "생필품 예산 주의보! 쟁여두기 쇼핑은 다음 달로 미뤄볼까요?");
        MESSAGES.put("8:90", "생활비 바닥 경보! 필수 생필품 외에는 잠시 구매 동결!");

        // 9. 뷰티
        MESSAGES.put("9:50", "뷰티 예산 절반 사용! 세일 기간까지 지갑을 지켜볼까요?");
        MESSAGES.put("9:70", "화장대 예산 소진 중! 꼭 필요한 기본 케어 용품만 챙기기로 해요");
        MESSAGES.put("9:90", "뷰티 예산 바닥! 화장품 한 방울까지 싹싹 비워 쓸 때입니다");

        // 10. 교육
        MESSAGES.put("10:50", "자기계발/교육 예산 50% 소진! 투자한 만큼 알차게 배우고 계시죠?");
        MESSAGES.put("10:70", "교육비 예산 주의! 이번 달 구매한 강의나 도서는 완강/완독 도전!");
        MESSAGES.put("10:90", "교육 예산 임계점! 새로운 교재/강의는 다음 달 수강신청으로");

        // 11. 반려동물
        MESSAGES.put("11:50", "반려동물 예산 절반 소진! 사료/간식 잔여량을 체크해 주세요");
        MESSAGES.put("11:70", "반려동물 예산 주의보! 장난감 새로 사기보다 신나게 놀아주기!");
        MESSAGES.put("11:90", "반려동물 예산 한계! 당분간 귀여운 간식 페스티벌은 잠시 휴식!");

        // 12. 여행
        MESSAGES.put("12:50", "여행 예산 50% 달성! 숙소/교통 최저가 비교 잊지 마세요");
        MESSAGES.put("12:70", "여행 지출 급증! 현지 액티비티나 식비 예산을 재조정해 보세요");
        MESSAGES.put("12:90", "여행 예산 초과 직전! 여행지에서 기념품 쇼핑은 잠시 참아주세요");

        // 13. 기타
        MESSAGES.put("13:50", "기타 지출 예산 50% 달성! 어디로 샜는지 지출 내역을 한번 쓱 둘러보세요");
        MESSAGES.put("13:70", "기타 예산 증가 중! 세어나가는 소액 결제가 없는지 확인해 볼까요?");
        MESSAGES.put("13:90", "기타 예산 바닥! 주머니 속 숨은 지출을 완벽하게 통제해 주세요");

        // 적금 자동이체 알림
        PAY_TITLE_MAP.put("PAY_UPCOMING", "오늘은 적금 자동 이체일이에요!");
        PAY_TITLE_MAP.put("PAY_SUCCESS", "적금 자동이체가 성공했어요!");
        PAY_TITLE_MAP.put("PAY_FAIL", "적금 자동이체가 실패했어요!");

        PAY_BODY_MAP.put("PAY_UPCOMING", "오늘 오전 10시에 출금될 예정이에요.");
        PAY_BODY_MAP.put("PAY_SUCCESS", "정상적으로 이체되었어요!");
        PAY_BODY_MAP.put("PAY_FAIL", "잔액이 부족하여 이체되지 않았어요.");
    }

    // 푸시 제목 - "식비 예산 50% 소진"
    public static String getTitle(Long categoryId, int thresholdRate) {
        String name = CATEGORY_NAMES.getOrDefault(categoryId, "지출");
        return name + " 예산 " + thresholdRate + "% 소진";
    }

    // 푸시 본문 - 매핑에 없으면 기본 문구로 대체 (NPE 방지)
    public static String getBody(Long categoryId, int thresholdRate) {
        String key = categoryId + ":" + thresholdRate;
        String message = MESSAGES.get(key);
        if (message == null) {
            return getTitle(categoryId, thresholdRate) + "! 지출 내역을 확인해 보세요";
        }
        return message;
    }

    // 적금 알림 제목 - 적금 도메인에서 processNotification 호출 시 사용
    public static String getPaymentTitle(String typeCode) {
        return PAY_TITLE_MAP.getOrDefault(typeCode, "적금 알림");
    }

    // 적금 알림 본문
    public static String getPaymentBody(String typeCode) {
        return PAY_BODY_MAP.getOrDefault(typeCode, "적금 알림입니다.");
    }

    // 카테고리명만 필요할 때
    public static String getCategoryName(Long categoryId) {
        return CATEGORY_NAMES.getOrDefault(categoryId, "기타");
    }
}