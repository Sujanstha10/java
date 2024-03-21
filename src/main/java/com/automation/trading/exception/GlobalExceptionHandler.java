package com.automation.trading.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.utility.ResponseGenerator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NullPointerException.class)
	public static <T> ResponseEntity<ResponseDTO<T>> handleNullPointerException(NullPointerException exception) {
		log.error("Exception occurred. Cause: ", exception);
		return ResponseGenerator.generateInternalServerErrorResponse(exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public static <T> ResponseEntity<ResponseDTO<T>> handleException(Exception exception) {
		log.error("Exception occurred. Cause: ", exception);
		return ResponseGenerator.generateInternalServerErrorResponse(exception.getMessage());
	}

}
