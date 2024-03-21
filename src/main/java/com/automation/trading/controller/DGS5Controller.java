
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
import com.automation.trading.domain.calculation.DGS5Calculation;
import com.automation.trading.service.DGS5Service;

@RestController
public class DGS5Controller {

	@Autowired
	private DGS5Service dgs5Service;

	@Transactional

	@GetMapping(UrlMappings.RATE_OF_CHANGE_DGS5)
	public ResponseEntity<ResponseDTO> calculateRoc() {

		dgs5Service.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional

	@GetMapping(UrlMappings.ROLLING_AVG_THREE_DGS5)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<DGS5Calculation> dgs5CalculationResult = dgs5Service.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional

	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_DGS5)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<DGS5Calculation> dffCalculationResult = dgs5Service.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_DGS5)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<DGS5Calculation> dgs5CalculationsList = dgs5Service.updateRocChangeSignDgs5();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

}
