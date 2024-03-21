package com.automation.trading.common;

import java.time.LocalDateTime;

public class ResponseDTO<T> {
	private int status;
	private boolean success;
	private T data;
	private LocalDateTime timeStamp = LocalDateTime.now();
	private String message;

	public ResponseDTO(T data, String message, boolean success, int httpStatus) {
		super();
		this.data = data;
		this.message = message;
		this.success = success;
		this.status = httpStatus;
	}

	public ResponseDTO(T data) {
		this.data = data;
	}

	public ResponseDTO(int status, boolean success, T data, String message) {
		this.data = data;
		this.message = message;
		this.success = success;
		this.status = status;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public LocalDateTime getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(LocalDateTime timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
