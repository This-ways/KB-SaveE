package org.scoula.savings.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.scoula.savings.domain.SavingsProductVO;
import org.scoula.savings.domain.SavingsRateVO;
import org.scoula.savings.dto.api.FinlifeSavingsResponse;
import org.scoula.savings.mapper.SavingsMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavingsService {

    private final RestTemplate restTemplate;
    private final SavingsMapper savingsMapper; // 💡 DB 저장을 위한 Mapper 주입

    @Value("${finlife.api.key}")
    private String apiKey;

    private static final String KB_FIN_CO_NO = "0010927";

    @Transactional // 💡 수집 및 DB 저장 트랜잭션 보장
    public void printSavingsData() {
        log.info("==================== KB국민은행 전체 적금 수집 시작 ====================");

        Map<String, FinlifeSavingsResponse.BaseDto> kbProductMap = new LinkedHashMap<>();
        // 💡 하나의 상품 코드가 여러 적립 방식을 가질 수 있으므로 Set으로 관리 (중복 제거)
        Map<String, Set<String>> productTypeSetMap = new HashMap<>();
        List<FinlifeSavingsResponse.OptionDto> kbOptionList = new ArrayList<>();

        int currentPage = 1;
        int maxPage = 1;

        try {
            // 1. 시중은행 권역(020000) 동적 순회
            while (currentPage <= maxPage) {
                String url = "https://finlife.fss.or.kr/finlifeapi/savingProductsSearch.json"
                        + "?auth=" + apiKey.trim()
                        + "&topFinGrpNo=020000"
                        + "&pageNo=" + currentPage;

                FinlifeSavingsResponse response = restTemplate.getForObject(url, FinlifeSavingsResponse.class);

                if (response == null || response.getResult() == null) {
                    break;
                }

                if (response.getResult().getMaxPageNo() != null) {
                    try {
                        maxPage = Integer.parseInt(response.getResult().getMaxPageNo().trim());
                    } catch (NumberFormatException e) {
                        maxPage = 1;
                    }
                }

                // KB국민은행 상품 BaseList 수집
                if (response.getResult().getBaseList() != null) {
                    for (FinlifeSavingsResponse.BaseDto base : response.getResult().getBaseList()) {
                        if (KB_FIN_CO_NO.equals(base.getFinCoNo()) && !kbProductMap.containsKey(base.getFinPrdtCd())) {
                            kbProductMap.put(base.getFinPrdtCd(), base);
                        }
                    }
                }

                // KB국민은행 금리 OptionList 수집 및 Option별 적립방식(rsrv_type) 수집
                if (response.getResult().getOptionList() != null) {
                    for (FinlifeSavingsResponse.OptionDto opt : response.getResult().getOptionList()) {
                        if (kbProductMap.containsKey(opt.getFinPrdtCd())) {
                            kbOptionList.add(opt);

                            // 💡 OptionDto의 rsrvType 판별
                            String type = "정액적립식";
                            if ("F".equalsIgnoreCase(opt.getRsrvType()) ||
                                    (opt.getRsrvTypeNm() != null && opt.getRsrvTypeNm().contains("자유"))) {
                                type = "자유적립식";
                            }

                            // 상품 코드별 Set에 적립 방식을 누적 (둘 다 존재하면 둘 다 저장)
                            productTypeSetMap.computeIfAbsent(opt.getFinPrdtCd(), k -> new LinkedHashSet<>()).add(type);
                        }
                    }
                }

                currentPage++;
            }

            // 2. KB국민은행 적금 상품 (SAVINGS_PRODUCT) 출력 및 DB 저장
            log.info("\n------------------------------------------------------------------------------------------------------------------------");
            log.info("1. [SAVINGS_PRODUCT (적금상품)] KB국민은행 총 ({})건 수집 완료", kbProductMap.size());
            log.info("------------------------------------------------------------------------------------------------------------------------");

            // DB에 생성/갱신된 productId를 상품코드(finPrdtCd) 키로 저장해둘 Map (금리 FK 연동용)
            Map<String, Long> generatedProductIdMap = new HashMap<>();

            int productId = 1;
            for (FinlifeSavingsResponse.BaseDto base : kbProductMap.values()) {

                // 💡 수집된 적립 방식 Set을 쉼표로 연결 (예: "정액적립식, 자유적립식")
                Set<String> typeSet = productTypeSetMap.get(base.getFinPrdtCd());
                String productType;
                if (typeSet != null && !typeSet.isEmpty()) {
                    productType = String.join(", ", typeSet);
                } else {
                    productType = (base.getFinPrdtNm() != null && base.getFinPrdtNm().contains("자유")) ? "자유적립식" : "정액적립식";
                }

                // 💡 [예외 보완] KB내맘대로적금(010200100070)처럼 API에는 정액으로만 오지만 실제로는 겸용인 상품 예외 처리
                if ("010200100070".equals(base.getFinPrdtCd())) {
                    productType = "정액적립식, 자유적립식";
                }

                int minAmount = 0;
                long maxAmount = (base.getMaxLimit() != null) ? base.getMaxLimit() : 0L;
                int status = 10;
                String detailUrl = null;

                String feature = (base.getEtcNote() != null) ? base.getEtcNote().replaceAll("\r?\n", " ").trim() : "없음";
                String target = (base.getJoinMember() != null) ? base.getJoinMember().trim() : "제한없음";
                String interestPayType = (base.getIntrRateTypeNm() != null) ? base.getIntrRateTypeNm() : "만기일시지급식";

                log.info("[상품ID: {}] 금융사코드: {} | 상품코드: {} | 상품명: {} | 종류: {} | 최소납입액: {} | 최대납입액: {} | 상태: {} | URL: {}",
                        productId++,
                        base.getFinCoNo(),
                        base.getFinPrdtCd(),
                        base.getFinPrdtNm(),
                        productType,
                        minAmount,
                        maxAmount,
                        status,
                        detailUrl);

                log.info("   ├ [상품특징]: {}", feature);
                log.info("   ├ [가입대상]: {}", target);
                log.info("   └ [이자지급시기]: {}", interestPayType);

                // 💡 DB 저장: SavingsProductVO 객체 생성 후 upsert
                SavingsProductVO productVO = SavingsProductVO.builder()
                        .companyCode(base.getFinCoNo())
                        .productCode(base.getFinPrdtCd())
                        .productName(base.getFinPrdtNm())
                        .productType(productType)
                        .minAmount(minAmount)
                        .maxAmount(maxAmount)
                        .feature(feature)
                        .target(target)
                        .interestPayType(interestPayType)
                        .detailUrl(detailUrl)
                        .status(status)
                        .build();

                savingsMapper.upsertProduct(productVO);

                // 저장/갱신 완료 후 자동 발급된 productId 가져오기
                Long dbProductId = productVO.getProductId();
                generatedProductIdMap.put(base.getFinPrdtCd(), dbProductId);

                // 💡 재수집 시 금리 중복 방지를 위해 기존 해당 상품의 금리 데이터 삭제
                savingsMapper.deleteRatesByProductId(dbProductId);
            }

            // 3. KB국민은행 적금 금리 (SAVINGS_RATE) 출력 및 DB 저장
            int todayDateInt = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));

            log.info("\n------------------------------------------------------------------------------------------------------------------------");
            log.info("2. [SAVINGS_RATE (적금금리)] KB국민은행 총 ({})건 수집 완료", kbOptionList.size());
            log.info("------------------------------------------------------------------------------------------------------------------------");

            for (FinlifeSavingsResponse.OptionDto opt : kbOptionList) {
                FinlifeSavingsResponse.BaseDto base = kbProductMap.get(opt.getFinPrdtCd());
                String productName = (base != null) ? base.getFinPrdtNm() : "알 수 없는 상품";

                // Option 개별 건에 해당하는 적립 방식
                String optType = ("F".equalsIgnoreCase(opt.getRsrvType()) ||
                        (opt.getRsrvTypeNm() != null && opt.getRsrvTypeNm().contains("자유"))) ? "자유적립식" : "정액적립식";

                Double minRate = (opt.getIntrRate() != null) ? opt.getIntrRate() : 0.0;
                Double maxRate = (opt.getIntrRate2() != null) ? opt.getIntrRate2() : minRate;
                int saveTerm = (opt.getSaveTrm() != null) ? Integer.parseInt(opt.getSaveTrm()) : 0;

                log.info("[금리] 상품명: [{}] | 상품코드: {} | 적립방식: {} | 가입기간: {}개월 | 최저연이율: {}% | 최고연이율: {}% | 적용시작일: {}",
                        productName,
                        opt.getFinPrdtCd(),
                        optType,
                        saveTerm,
                        minRate,
                        maxRate,
                        todayDateInt);

                // 💡 DB 저장: SavingsRateVO 객체 생성 후 insert (Double -> BigDecimal 타입 변환 적용)
                Long dbProductId = generatedProductIdMap.get(opt.getFinPrdtCd());
                if (dbProductId != null) {
                    SavingsRateVO rateVO = SavingsRateVO.builder()
                            .productId(dbProductId)
                            .saveTerm(saveTerm)
                            .minRate(BigDecimal.valueOf(minRate)) // 💡 BigDecimal로 변환
                            .maxRate(BigDecimal.valueOf(maxRate)) // 💡 BigDecimal로 변환
                            .startDate(todayDateInt)
                            .endDate(null)
                            .build();

                    savingsMapper.insertRate(rateVO);
                }
            }

            log.info("================================================================================================------------------------");

        } catch (Exception e) {
            log.error("수집 및 DB 저장 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("DB 저장 실패로 인한 트랜잭션 롤백", e);
        }
    }
}