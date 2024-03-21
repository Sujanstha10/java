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
import com.automation.trading.domain.calculation.CPIAUCSLCalculation;
import com.automation.trading.service.CPIAUCSLService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class CPIAUCSLController {

	@Autowired
	private CPIAUCSLService cpiaucslService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_CPIAUCSL)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		cpiaucslService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_CPIAUCSL)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<CPIAUCSLCalculation> cpiaucslCalculationResult = cpiaucslService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_CPIAUCSL)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<CPIAUCSLCalculation> cpiaucslCalculationResult = cpiaucslService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_CPIAUCSL)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<CPIAUCSLCalculation> cpiaucslCalculationsList = cpiaucslService.updateRocChangeSignDff();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
