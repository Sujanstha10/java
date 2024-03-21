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
import com.automation.trading.domain.calculation.T10YIECalculation;
import com.automation.trading.service.T10YIEService;

@RestController
public class T10YIEController {

	@Autowired
	private T10YIEService t10yieService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_T10YIE)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		t10yieService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_T10YIE)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<T10YIECalculation> t10yieCalculationResult = t10yieService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_T10YIE)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvgDff() {
		List<T10YIECalculation> dffCalculationResult = t10yieService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_T10YIE)
	public ResponseEntity<ResponseDTO> updateRocChangeDirectionDff() {
		List<T10YIECalculation> t10yieCalculationsList = t10yieService.updateRocChangeSignT10YIE();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
