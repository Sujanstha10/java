/*
 * package com.automation.trading.controller;
 * 
 * import java.util.List;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.http.HttpStatus; import
 * org.springframework.http.ResponseEntity; import
 * org.springframework.transaction.annotation.Transactional; import
 * org.springframework.web.bind.annotation.GetMapping; import
 * org.springframework.web.bind.annotation.RestController;
 * 
 * import com.automation.trading.common.ResponseDTO; import
 * com.automation.trading.common.ResponseHandler; import
 * com.automation.trading.constants.UrlMappings; import
 * com.automation.trading.domain.calculation.T5YIFRCalculation; import
 * com.automation.trading.service.T5YIFRService;
 * 
 * @RestController public class T5YIFRController {
 * 
 * @Autowired private T5YIFRService t5yifrService;
 * 
 * @Transactional
 * 
 * @GetMapping(UrlMappings.RATE_OF_CHANGE_T5YIFR) public
 * ResponseEntity<ResponseDTO> calculateRoc() { t5yifrService.calculateRoc();
 * return ResponseHandler.generateResponse(HttpStatus.OK, true, "data",
 * "Message"); }
 * 
 * @Transactional
 * 
 * @GetMapping(UrlMappings.ROLLING_AVG_THREE_T5YIFR) public
 * ResponseEntity<ResponseDTO> calculateRollingAvgThreeMonth() {
 * List<T5YIFRCalculation> t5yifrCalculationResult =
 * t5yifrService.calculateRollAvgThreeMonth(); return
 * ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message"); }
 * 
 * @Transactional
 * 
 * @GetMapping(UrlMappings.ROC_ANNUAL_ROLLING_AVG_T5YIFR) public
 * ResponseEntity<ResponseDTO> calculateRocAnnualRollingAvgDff() {
 * List<T5YIFRCalculation> dffCalculationResult =
 * t5yifrService.calculateRocRollingAnnualAvg(); return
 * ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message"); }
 * 
 * @GetMapping(UrlMappings.RATE_OF_CHANGE_SIGN_T5YIFR) public
 * ResponseEntity<ResponseDTO> updateRocChangeDirectionDff() {
 * List<T5YIFRCalculation> t5yifrCalculationsList =
 * t5yifrService.updateRocChangeSignDff(); return
 * ResponseHandler.generateResponse(HttpStatus.OK, true, "data", "Message"); }
 * 
 * }
 */