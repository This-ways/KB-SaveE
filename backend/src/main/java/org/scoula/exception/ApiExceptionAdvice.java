package org.scoula.exception;

import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.config.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.NoSuchElementException;


@RestControllerAdvice
@Log4j2
@Order(2)
public class ApiExceptionAdvice {
    // 404 에러
    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<String> handleIllegalArgumentException(NoSuchElementException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("해당 ID의 요소가 없습니다.");
    }

    // 409 에러 - UNIQUE 제약 위반 (예: GOAL 중복 등록)
    @ExceptionHandler(DuplicateKeyException.class)
    protected ResponseEntity<String> handleDuplicateKeyException(DuplicateKeyException e) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("이미 존재하는 데이터입니다.");
    }

    // 400 에러 - 잘못된 요청 상태 (예: 1~7일 수정기간 위반, 아이디 중복 등)
    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body(e.getMessage());
    }
    // 500 에러 - 예상 못한 시스템/통신 장애만 여기로 옴 (업무 로직 에러는 위에서 이미 처리됨)
    // 스택트레이스/DB 예외 메시지 등 개발자용 상세 정보는 로그로만 남기고, 클라이언트에는
    // 절대 노출하지 않는다 (SQL 문법, 컬럼명, 예외 클래스명 등이 그대로 나가면 보안상 위험함)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<String> handleException(Exception e) {
        log.error("처리되지 않은 서버 오류", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Content-Type", "text/plain;charset=UTF-8")
                .body("일시적인 오류가 발생했어요. 잠시 후 다시 시도해 주세요.");
    }

}