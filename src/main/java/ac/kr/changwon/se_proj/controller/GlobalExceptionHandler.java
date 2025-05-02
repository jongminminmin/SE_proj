package ac.kr.changwon.se_proj.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String , Object>> handleIllegalArgument(IllegalArgumentException e){
        Map<String , Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String , Object>> handleException(Exception e){
        Map<String , Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", e.getMessage());
        //에외 찍히는 종류
        body.put("Exception", e.getClass().getName());

        //로그에 스택트레이스
        log.error("Unhandled exception caught:", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

}
