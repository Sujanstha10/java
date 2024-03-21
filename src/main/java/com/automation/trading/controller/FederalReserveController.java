package com.automation.trading.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.common.ResponseHandler;
import com.automation.trading.constants.UrlMappings;
import com.automation.trading.domain.calculation.GdpCalculation;
import com.automation.trading.service.GdpRateOfChangeService;

@Controller
public class FederalReserveController {

	@Autowired
	private GdpRateOfChangeService gdpRateOfChangeService;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_GDP)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		List<GdpCalculation> gdpCalculationResult = gdpRateOfChangeService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_MONTH)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonthGdp() {
		List<GdpCalculation> gdpCalculationResult = gdpRateOfChangeService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_GDP)
	public ResponseEntity<ResponseDTO> updateRocChangeDirectionGdp() {
		List<GdpCalculation> gdpCalculationsList = gdpRateOfChangeService.updateRocChangeSignGdp();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_GDP)
	public ResponseEntity<ResponseDTO> calculateRocAnnaulRollingAvgGdp() {
		List<GdpCalculation> gdpCalculationResult = gdpRateOfChangeService.calculateRocRollingAnnualAvgGdp();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

//	@Scheduled(cron = "0 0 12 * * ?")
//	@GetMapping(UrlMappings.UPDATED_GDP)
//	public ResponseEntity<ResponseDTO> fetchUpdatedGdp() {
//		gdpRateOfChangeService.getLatestGdpRecords();
//		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
//	}

}
