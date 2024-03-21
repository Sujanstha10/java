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
import com.automation.trading.domain.calculation.M2VCalculation;
import com.automation.trading.service.M2VService;

@RestController
public class M2VController {
	
	@Autowired
	private M2VService m2vService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_M2V)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		m2vService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_M2V)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<M2VCalculation> m2vCalculationResult = m2vService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_M2V)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<M2VCalculation> dffCalculationResult = m2vService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_M2V)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<M2VCalculation> m2vCalculationsList = m2vService.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
