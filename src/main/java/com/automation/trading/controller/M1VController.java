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
import com.automation.trading.domain.calculation.M1VCalculation;
import com.automation.trading.service.M1VService;

@RestController
public class M1VController {
	
	@Autowired
	private M1VService m1vService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_M1V)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		m1vService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_M1V)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<M1VCalculation> m1vCalculationResult = m1vService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_M1V)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<M1VCalculation> dffCalculationResult = m1vService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_M1V)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {

		List<M1VCalculation> m1vCalculationsList = m1vService.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
