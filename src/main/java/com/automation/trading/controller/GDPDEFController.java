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
import com.automation.trading.domain.calculation.GDPDEFCalculation;
import com.automation.trading.service.GDPDEFService;

@RestController
public class GDPDEFController {
	
	@Autowired
	private GDPDEFService gdpdefService; 

	@GetMapping(UrlMappings.RATE_OF_CHANGE_GDPDEF)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		gdpdefService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");

	}
	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_GDPDEF)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<GDPDEFCalculation> gdpdefCalculationResult = gdpdefService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_GDPDEF)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<GDPDEFCalculation> dffCalculationResult = gdpdefService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_GDPDEF)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<GDPDEFCalculation> gdpdefCalculationsList = gdpdefService.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}
	

}
