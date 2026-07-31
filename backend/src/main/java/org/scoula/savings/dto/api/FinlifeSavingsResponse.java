package org.scoula.savings.dto.api;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class FinlifeSavingsResponse {

    @JsonProperty("result")
    private Result result;

    @Getter @Setter
    public static class Result {

        @JsonProperty("prdt_div")
        private String prdtDiv;

        @JsonProperty("total_count")
        private String totalCount;

        @JsonProperty("max_page_no")
        private String maxPageNo;

        @JsonProperty("now_page_no")
        private String nowPageNo;

        @JsonProperty("err_cd")
        private String errCd;

        @JsonProperty("err_msg")
        private String errMsg;

        @JsonProperty("baseList")
        private List<BaseDto> baseList;

        @JsonProperty("optionList")
        private List<OptionDto> optionList;
    }

    @Getter @Setter
    public static class BaseDto {
        @JsonProperty("fin_co_no") private String finCoNo;       // 금융회사코드
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;   // 상품코드
        @JsonProperty("fin_prdt_nm") private String finPrdtNm;   // 상품명
        @JsonProperty("rsrv_type") private String rsrvType;       // 적립유형 (S: 정액, F: 자유)
        @JsonProperty("rsrv_type_nm") private String rsrvTypeNm; // 적립유형명 (정액적립식 / 자유적립식)
        @JsonProperty("max_limit") private Long maxLimit;        // 최고한도
        @JsonProperty("etc_note") private String etcNote;        // 상품특징 / 기타유의사항
        @JsonProperty("join_member") private String joinMember;  // 가입대상
        @JsonProperty("intr_rate_type_nm") private String intrRateTypeNm; // 이자지급방식 / 설명
    }

    @Getter @Setter
    public static class OptionDto {
        @JsonProperty("fin_prdt_cd") private String finPrdtCd;
        @JsonProperty("save_trm") private String saveTrm;
        @JsonProperty("intr_rate") private Double intrRate;
        @JsonProperty("intr_rate2") private Double intrRate2;
        @JsonProperty("rsrv_type") private String rsrvType;         // 💡 "S": 정액, "F": 자유
        @JsonProperty("rsrv_type_nm") private String rsrvTypeNm;   // 💡 "정액적립식", "자유적립식"
    }
}
