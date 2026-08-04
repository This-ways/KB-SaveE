package org.scoula.notification.mapper;

import org.apache.ibatis.annotations.Param;
import org.scoula.notification.domain.NotificationVO;

import java.util.List;

public interface NotificationMapper {

    // 알림 기록 저장 - UNIQUE 위반 시 0 반환 (INSERT IGNORE)
    int insert(NotificationVO vo);

    // 유저의 알림 전체 조회 (최신순)
    List<NotificationVO> selectByUserId(Long userId);

    // 특정 월의 알림 조회
    List<NotificationVO> selectByUserIdAndMonth(@Param("userId") Long userId,
                                                @Param("targetMonth") String targetMonth);

    // 목표(GOAL) 변경 시 해당 월/카테고리 알림 삭제 -> 임계값 알림이 다시 발송될 수 있게 함
    int deleteByCategoryAndMonth(@Param("userId") Long userId,
                                 @Param("categoryId") Long categoryId,
                                 @Param("targetMonth") String targetMonth);

    // 이번 달 이전 알림 전체 삭제 (예산 설정 시 호출, 알림 무한 누적 방지)
    int deleteBeforeMonth(@Param("userId") Long userId,
                          @Param("currentMonth") String currentMonth);

    // 푸시 수신 동의 여부 (user 테이블) - 거부면 DB 저장만 하고 FCM은 건너뜀
    Boolean selectPushEnabled(Long userId);

    // 해당 월 카테고리 목표 금액 (goal 테이블) - 목표 없으면 null
    Integer selectGoalAmount(@Param("userId") Long userId,
                             @Param("categoryId") Long categoryId,
                             @Param("targetMonth") String targetMonth);

    // 해당 월 카테고리 누적 지출액 (transaction 테이블) - 지출만 합산
    int selectMonthlySpending(@Param("userId") Long userId,
                              @Param("categoryId") Long categoryId,
                              @Param("targetMonth") String targetMonth);
}