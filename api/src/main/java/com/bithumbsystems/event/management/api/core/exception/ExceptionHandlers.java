package com.bithumbsystems.event.management.api.core.exception;

import com.bithumbsystems.event.management.api.core.model.enums.ErrorCode;
import com.bithumbsystems.event.management.api.core.model.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class ExceptionHandlers {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<Mono<?>> serverExceptionHandler(Exception ex) {
        log.error(ex.getMessage(), ex);
        ErrorData errorData = new ErrorData(ErrorCode.UNKNOWN_ERROR);
        return ResponseEntity.internalServerError().body(Mono.just(new ErrorResponse(errorData)));
    }
}
