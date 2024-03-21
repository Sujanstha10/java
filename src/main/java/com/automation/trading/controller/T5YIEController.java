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
import com.automation.trading.domain.calculation.T5YIECalculation;
import com.automation.trading.service.T5YIEService;

@RestController
public class T5YIEController {

	@Autowired
	private T5YIEService t5yieService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_T5YIE)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		t5yieService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_T5YIE)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<T5YIECalculation> t5yieCalculationResult = t5yieService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_T5YIE)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<T5YIECalculation> dffCalculationResult = t5yieService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_T5YIE)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<T5YIECalculation> t5yieCalculationsList = t5yieService.updateRocChangeSignT5YIE();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
