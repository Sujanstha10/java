package com.automation.trading.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseHandler {
	private ResponseHandler() {
	}

	public static <T> ResponseEntity<ResponseDTO> generateResponse(HttpStatus status, boolean isSuccess, T responsObj,
			String message) {
		ResponseDTO responseDTO = new ResponseDTO(status.value(), isSuccess, responsObj, message);
		return new ResponseEntity<>(responseDTO, status);
	}
}


