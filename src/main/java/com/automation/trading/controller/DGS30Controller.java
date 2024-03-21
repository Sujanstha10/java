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
import com.automation.trading.domain.calculation.DGS30Calculation;
import com.automation.trading.service.DGS30Service;

@RestController
public class DGS30Controller {
	
	@Autowired
	private DGS30Service dgs30Service;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_DGS30)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		dgs30Service.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_DGS30)

	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<DGS30Calculation> dgs30CalculationResult = dgs30Service.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_DGS30)

	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<DGS30Calculation> dffCalculationResult = dgs30Service.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_DGS30)

	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<DGS30Calculation> dgs30CalculationsList = dgs30Service.updateRocChangeSign();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
