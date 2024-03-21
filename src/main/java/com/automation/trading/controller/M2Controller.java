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
import com.automation.trading.domain.calculation.M2Calculation;
import com.automation.trading.service.M2Service;

@RestController
public class M2Controller {
	
	@Autowired
	private M2Service m2Service;
	
	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_M2)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		m2Service.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}
	
	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_M2)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<M2Calculation> m2CalculationResult = m2Service.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_M2)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<M2Calculation> dffCalculationResult = m2Service.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_M2)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<M2Calculation> m2CalculationsList = m2Service.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
