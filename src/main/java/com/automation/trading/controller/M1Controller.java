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
import com.automation.trading.domain.calculation.M1Calculation;
import com.automation.trading.service.M1Service;

@RestController
public class M1Controller {

	@Autowired
	private M1Service m1Service;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_M1)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		m1Service.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_M1)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<M1Calculation> m1CalculationResult = m1Service.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_M1)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<M1Calculation> dffCalculationResult = m1Service.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_M1)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<M1Calculation> m1CalculationsList = m1Service.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
