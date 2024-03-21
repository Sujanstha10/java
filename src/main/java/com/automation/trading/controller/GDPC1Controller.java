package com.automation.trading.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.common.ResponseHandler;
import com.automation.trading.constants.UrlMappings;
import com.automation.trading.domain.calculation.GDPC1Calculation;
import com.automation.trading.service.GDPC1Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class GDPC1Controller {

	@Autowired
	private GDPC1Service gdpc1Service;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_GDPC1)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		gdpc1Service.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_GDPC1)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<GDPC1Calculation> gdpc1CalculationResult = gdpc1Service.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_GDPC1)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<GDPC1Calculation> dffCalculationResult = gdpc1Service.calculateRocRollingAnnualAvgGDPC1();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_GDPC1)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<GDPC1Calculation> gdpc1CalculationsList = gdpc1Service.updateRocChangeSignGDPC1();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
