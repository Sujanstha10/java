package com.automation.trading.utility;

import java.io.Serializable;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseDTO<T> implements Serializable {

	private static final long serialVersionUID = -6072518716971093796L;
	private T data;
	private String message;
	private boolean success;
	private int httpStatus;
	private long timestamp = System.currentTimeMillis();

	/**
	 * @param data
	 * @param message
	 * @param success
	 * @param httpStatus
	 */
	public ResponseDTO(T data, String message, boolean success, int httpStatus) {
		super();
		this.data = data;
		this.message = message;
		this.success = success;
		this.httpStatus = httpStatus;
	}

	public ResponseDTO(T data) {
		super();
		this.data = data;
		this.success = true;
		this.httpStatus = HttpStatus.OK.value();
	}

	public ResponseDTO(T data, int httpStatus) {
		super();
		this.data = data;
		this.success = true;
		this.httpStatus = httpStatus;
	}

}