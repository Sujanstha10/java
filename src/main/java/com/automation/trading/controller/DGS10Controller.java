package com.automation.trading.controller;

import com.automation.trading.common.ResponseDTO;
import com.automation.trading.common.ResponseHandler;
import com.automation.trading.constants.UrlMappings;
import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.service.DGS10Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DGS10Controller {

    @Autowired
    private DGS10Service dgs10Service;

    @Transactional

    @GetMapping(UrlMappings.RATE_OF_CHANGE_DGS10)
    public ResponseEntity<ResponseDTO> calculateRoc() {

        dgs10Service.calculateRoc();
        return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
    }

    @Transactional

    @GetMapping(UrlMappings.ROLLING_AVG_THREE_DGS10)
    public ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
        List<DGS10Calculation> dgs10CalculationResult = dgs10Service.calculateRollAvgThreeMonth();
        return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
    }

    @Transactional

    @GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_DGS10)
    public ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvg() {
        List<DGS10Calculation> dffCalculationResult = dgs10Service.calculateRocRollingAnnualAvg();
        return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message");
    }

    @GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_DGS10)
    public ResponseEntity<ResponseDTO> updateRocChangeDirection() {
        List<DGS10Calculation> dgs10CalculationsList = dgs10Service.updateRocChangeSignDgs10();
        return ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Rate of Change Sign for DGS10");
    }

}
