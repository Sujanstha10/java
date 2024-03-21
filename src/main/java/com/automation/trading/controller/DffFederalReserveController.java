package com.automation.trading.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.common.ResponseHandler;
import com.automation.trading.constants.UrlMappings;
import com.automation.trading.domain.calculation.DffCalculation;
import com.automation.trading.service.DffRateOfChangeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class DffFederalReserveController {

	@Autowired
	DffRateOfChangeService dffRateOfChangeService;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_DFF)
	public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
		List<DffCalculation> dffCalculationsList = dffRateOfChangeService.updateRocChangeSignDFF();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_DFF)
	public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
		List<DffCalculation> dffCalculationResult = dffRateOfChangeService.calculateRocRollingAnnualAvgDFF();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_MONTH_DFF)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
		List<DffCalculation> dffCalculationResult = dffRateOfChangeService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_DFF)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		List<DffCalculation> dffCalculationResult = dffRateOfChangeService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}
//
//	@GetMapping(UrlMappings.UPDATED_DFF)
//	public ResponseEntity<ResponseDTO> fetchUpdatedDff() {
//		dffRateOfChangeService.getLatestDFFRecords();
//		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
//	}

}
