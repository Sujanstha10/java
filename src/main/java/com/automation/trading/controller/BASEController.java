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
import com.automation.trading.domain.calculation.BASECalculation;
import com.automation.trading.service.BASEService;

@RestController
public class BASEController {

	@Autowired
	private BASEService baseService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_BASE)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		baseService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_BASE)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<BASECalculation> baseCalculationResult = baseService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_BASE)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<BASECalculation> dffCalculationResult = baseService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_BASE)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<BASECalculation> baseCalculationsList = baseService.updateRocChangeSignDff();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}
}