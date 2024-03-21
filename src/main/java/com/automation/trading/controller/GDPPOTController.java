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
import com.automation.trading.domain.calculation.GDPPOTCalculation;
import com.automation.trading.service.GDPPOTService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class GDPPOTController {

	@Autowired
	private GDPPOTService gdppotService;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_GDPPOT)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		gdppotService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_GDPPOT)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<GDPPOTCalculation> gdppotCalculationResult = gdppotService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_GDPPOT)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvgDff() {
		List<GDPPOTCalculation> dffCalculationResult = gdppotService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_GDPPOT)
	public ResponseEntity<ResponseDTO> updateRocChangeDirectionDff() {
		List<GDPPOTCalculation> gdppotCalculationsList = gdppotService.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
