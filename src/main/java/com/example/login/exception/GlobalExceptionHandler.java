package com.example.login.exception;

import com.example.login.dto.LoginResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 * 모든 에러 응답을 LoginResponse 형식으로 통일합니다. (구조화된 출력)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 입력값 유효성 검사 실패 시 (길이 초과, 빈 값 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<LoginResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity.badRequest().body(LoginResponse.error(errorMessage));
    }

    // 그 외 예기치 못한 서버 오류
    @ExceptionHandler(Exception.class)
    public ResponseEntity<java.util.Map<String, Object>> handleException(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError()
            .body(java.util.Map.of("success", false, "message", "서버 오류가 발생했습니다."));
    }
}
