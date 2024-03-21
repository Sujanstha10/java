package com.automation.trading.utility;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.automation.trading.common.ResponseDTO;

public class ResponseGenerator {

	/**
	 * This method will return the controller response.
	 *
	 * @return {@link ResponseEntity} of {@link ResponseDTO}
	 */
	public static <T> ResponseEntity<ResponseDTO<T>> generateSuccessResponse(T data) {
		ResponseDTO<T> responseDTO = new ResponseDTO(data);
		return new ResponseEntity<ResponseDTO<T>>(responseDTO, HttpStatus.OK);
	}

	/*
	 * public static <T> ResponseEntity<Object>
	 * generateBadRequestResponse(List<FieldErrorDTO> fieldErrors) { ResponseDTO<T>
	 * responseDTO = new ResponseDTO<>(fieldErrors); return new
	 * ResponseEntity<>(responseDTO, HttpStatus.BAD_REQUEST); }
	 */

	public static <T> ResponseEntity<ResponseDTO<T>> generateConflictResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO(null, message, false, HttpStatus.CONFLICT.value());
		return new ResponseEntity<ResponseDTO<T>>(responseDTO, HttpStatus.CONFLICT);
	}

	public static <T> ResponseEntity<ResponseDTO<T>> generateBadRequestResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO(null, message, false, HttpStatus.BAD_REQUEST.value());
		return new ResponseEntity<ResponseDTO<T>>(responseDTO, HttpStatus.BAD_REQUEST);
	}

	public static <T> ResponseEntity<ResponseDTO<T>> generateInternalServerErrorResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO<T>(null, message, false, HttpStatus.INTERNAL_SERVER_ERROR.value());
		return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public static <T> ResponseEntity<ResponseDTO<T>> generateUnprocessableEntityResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO<T>(null, message, false, HttpStatus.UNPROCESSABLE_ENTITY.value());
		return new ResponseEntity<>(responseDTO, HttpStatus.UNPROCESSABLE_ENTITY);
	}

	public static <T> ResponseEntity<ResponseDTO<T>> generateAccessDeniedResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO<T>(null, message, false, HttpStatus.FORBIDDEN.value());
		return new ResponseEntity<>(responseDTO, HttpStatus.FORBIDDEN);
	}

	public static <T> ResponseEntity<ResponseDTO<T>> generateAuthenticationExceptionResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO<T>(null, message, false, HttpStatus.UNAUTHORIZED.value());
		return new ResponseEntity<>(responseDTO, HttpStatus.UNAUTHORIZED);
	}

	public static <T> ResponseEntity<Object> generateHttpMediaTypeNotSupportedResponse(String message) {
		ResponseDTO<T> responseDTO = new ResponseDTO<>(null, message, false, HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
		return new ResponseEntity<>(responseDTO, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
	}

}