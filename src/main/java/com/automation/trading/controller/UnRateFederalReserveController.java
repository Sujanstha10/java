package com.automation.trading.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.common.ResponseHandler;
import com.automation.trading.constants.UrlMappings;
import com.automation.trading.domain.calculation.UnRateCalculation;
import com.automation.trading.repository.UnRateRepostiory;
import com.automation.trading.service.UnRateRateOfChangeService;

@Controller
public class UnRateFederalReserveController {

	private Logger logger = LoggerFactory.getLogger(FederalReserveController.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private UnRateRepostiory unRateRepostiory;

	@Autowired
	UnRateRateOfChangeService unRateRateOfChangeService;

	@Value("${quandl.host.url}")
	private String QUANDL_HOST_URL;

	@Value("${quandl.api.key.value}")
	private String QUANDL_API_KEY_VALUE;

	@Value("${quandl.api.key.name}")
	private String QUANDL_API_KEY_NAME;

	@Value("${quandl.data.format}")
	private String QUANDL_DATA_FORMAT;

	@Transactional
	@GetMapping(UrlMappings.ROLLING_AVG_THREE_MONTH_UNRATE)
	public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonthUnRate() {
		List<UnRateCalculation> unRateCalculationResult = unRateRateOfChangeService.calculateRollAvgThreeMonth();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_UNRATE)
	public ResponseEntity<ResponseDTO> updateRocChangeDirectionUnRate() {
		List<UnRateCalculation> unRateCalculationsList = unRateRateOfChangeService.updateRocChangeSignUnRate();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_UNRATE)
	public ResponseEntity<ResponseDTO> calculateRocAnnaulRollingAvgUnRate() {
		List<UnRateCalculation> unRateCalculationResult = unRateRateOfChangeService.calculateRocRollingAnnualAvg();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

	@Transactional
	@GetMapping(UrlMappings.RATE_OF_CHANGE_UNRATE)
	public ResponseEntity<ResponseDTO> calculateRoc() {
		List<UnRateCalculation> unRateCalculationResult = unRateRateOfChangeService.calculateRoc();
		return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
	}

//	
//
//    @Scheduled(cron = "0 0 12 * * ?")
//    @GetMapping(UrlMappings.UPDATED_UNRATE)
//    public ResponseEntity<ResponseDTO>fetchUpdatedUnRate(){
//        unRateRateOfChangeService.getLatestUnRateRecords();
//        return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
//    }

}
