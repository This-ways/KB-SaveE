package org.scoula.savings.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper {

    // 1. 가입 확인 화면(Output 1)에 보여줄 출금 예금계좌번호 조회
    String selectAccountNoByUserId(@Param("userId") Long userId);

    // 2. 최종 가입(Output 2) 시 DB에 넣을 예금계좌 PK(deposit_id) 조회
    Long selectDepositIdByUserId(@Param("userId") Long userId);

    // 예금 계좌 잔액 조회
    Long selectBalanceByDepositId(@Param("depositId") Long depositId);

    // 예금 계좌 잔액 차감 (출금)
    int withdrawBalance(@Param("depositId") Long depositId, @Param("amount") long amount);
}
