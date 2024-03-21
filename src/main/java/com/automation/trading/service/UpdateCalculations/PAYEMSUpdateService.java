package com.automation.trading.service.UpdateCalculations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import com.automation.trading.domain.fred.PAYEMS;
import com.automation.trading.service.FederalEmploymentService;
import com.automation.trading.utility.RestUtility;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.PAYEMSCalculation;
import com.automation.trading.repository.PAYEMSCalculationRepository;
import com.automation.trading.repository.PAYEMSRepository;
import com.automation.trading.service.PAYEMSService;

@Service
public class PAYEMSUpdateService {

    @Autowired
    private PAYEMSRepository payemsRepostiory;

    @Autowired
    private PAYEMSCalculationRepository payemsCalculationRepository;

    @Autowired
    private PAYEMSService payemsRateOfChangeService;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    RestUtility restUtility;

    @Value("${quandl.host.url}")
    private String QUANDL_HOST_URL;

    @Value("${quandl.api.key.value}")
    private String QUANDL_API_KEY_VALUE;

    @Value("${quandl.api.key.name}")
    private String QUANDL_API_KEY_NAME;

    @Value("${quandl.data.format}")
    private String QUANDL_DATA_FORMAT;

    private Logger logger = LoggerFactory.getLogger(PAYEMSUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(payemsCalculationRepository.findAny())) {
            payemsRateOfChangeService.calculateRoc();
            payemsRateOfChangeService.updateRocChangeSignPAYEMS();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<PAYEMS>> payemsListOpt = payemsRepostiory.findByRocFlagIsFalseOrderByDate();
        Optional<PAYEMS> prevPAYEMSOpt = payemsRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, PAYEMSCalculation> payemsCalculationHashMap = new HashMap<>();
        PAYEMSCalculation prevPAYEMSCalculationRow = new PAYEMSCalculation();

        List<PAYEMS> payemsList = new ArrayList<>();

        if (payemsListOpt.isPresent()) {
            payemsList = payemsListOpt.get();
            if (prevPAYEMSOpt.isPresent()) {
                payemsList.add(prevPAYEMSOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(payemsList, new FederalEmploymentService.SortByDatePAYEMS());
        List<PAYEMSCalculation> payemsCalculationReference = payemsCalculationRepository.findAll();
        List<PAYEMSCalculation> payemsCalculationModified = new ArrayList<>();
        Queue<PAYEMS> payemsQueue = new LinkedList<>();

        for (PAYEMSCalculation payemsCalculation : payemsCalculationReference) {
            payemsCalculationHashMap.put(payemsCalculation.getToDate(), payemsCalculation);
        }

        for (PAYEMS payems : payemsList) {
            PAYEMSCalculation tempPAYEMSCalculation = new PAYEMSCalculation();

            if (payemsQueue.size() == 2) {
                payemsQueue.poll();
            }
            payemsQueue.add(payems);

            if (payems.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<PAYEMS> queueIterator = payemsQueue.iterator();

            if (payemsCalculationHashMap.containsKey(payems.getDate())) {
                tempPAYEMSCalculation = payemsCalculationHashMap.get(payems.getDate());
            } else {
                tempPAYEMSCalculation.setToDate(payems.getDate());
            }

            while (queueIterator.hasNext()) {
                PAYEMS temp = queueIterator.next();
                temp.setRocFlag(true);
                if (payemsQueue.size() == 1) {
                    roc = 0f;
                    tempPAYEMSCalculation.setRoc(roc);
                    tempPAYEMSCalculation.setToDate(payems.getDate());
                    tempPAYEMSCalculation.setRocChangeSign(0);
                } else {
                    roc = (payems.getValue() / ((LinkedList<PAYEMS>) payemsQueue).get(0).getValue()) - 1;
                    tempPAYEMSCalculation.setRoc(roc);
                    tempPAYEMSCalculation.setToDate(payems.getDate());
                }

            }

            payemsCalculationModified.add(tempPAYEMSCalculation);
        }

        payemsList = payemsRepostiory.saveAll(payemsList);
        payemsCalculationModified = payemsCalculationRepository.saveAll(payemsCalculationModified);
        logger.debug("Added new PAYEMS row, " + payemsCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(payemsCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<PAYEMSCalculation>> payemsCalculationListOpt = payemsCalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<PAYEMSCalculation>> prevPAYEMSCalculationListOpt = payemsCalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<PAYEMSCalculation> payemsCalculationList = new ArrayList<>();

        if (payemsCalculationListOpt.isPresent()) {
            payemsCalculationList = payemsCalculationListOpt.get();
            if (prevPAYEMSCalculationListOpt.isPresent()) {
                payemsCalculationList.addAll(prevPAYEMSCalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(payemsCalculationList, new FederalEmploymentService.SortByDatePAYEMSCalculation());

        Queue<PAYEMSCalculation> payemsCalculationPriorityQueue = new LinkedList<PAYEMSCalculation>();
        for (PAYEMSCalculation payemsCalculation : payemsCalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (payemsCalculationPriorityQueue.size() == 4) {
                payemsCalculationPriorityQueue.poll();
            }
            payemsCalculationPriorityQueue.add(payemsCalculation);

            if (payemsCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<PAYEMSCalculation> queueIterator = payemsCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                PAYEMSCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            payemsCalculation.setRocAnnRollAvgFlag(true);
            payemsCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(payemsCalculationList);
        payemsCalculationList = payemsCalculationRepository.saveAll(payemsCalculationList);
        logger.info("New payems calculation record inserted" + payemsCalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month PAYEMS
     *
     * @return PAYEMSCalculation , updated PAYEMSCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(payemsCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<PAYEMSCalculation> payemsCalculationList = new ArrayList<>();
        Optional<List<PAYEMS>> payemsListOpt = payemsRepostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<PAYEMS>> prevPAYEMSListOpt = payemsRepostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<PAYEMSCalculation> payemsCalculationReference = payemsCalculationRepository.findAll();
        HashMap<Date, PAYEMSCalculation> payemsCalculationHashMap = new HashMap<>();
        List<PAYEMS> payemsList = new ArrayList<>();

        for (PAYEMSCalculation payemsCalculation : payemsCalculationReference) {
            payemsCalculationHashMap.put(payemsCalculation.getToDate(), payemsCalculation);
        }

        Queue<PAYEMS> payemsQueue = new LinkedList<>();

        if (payemsListOpt.isPresent()) {
            payemsList = payemsListOpt.get();
            if (prevPAYEMSListOpt.isPresent()) {
                payemsList.addAll(prevPAYEMSListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(payemsList, new FederalEmploymentService.SortByDatePAYEMS());

        for (PAYEMS payems : payemsList) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (payemsQueue.size() == 3) {
                payemsQueue.poll();
            }
            payemsQueue.add(payems);
            if (payems.getRollAverageFlag()) {
                continue;
            }

            Iterator<PAYEMS> queueItr = payemsQueue.iterator();

            PAYEMSCalculation tempPAYEMSCalculation = new PAYEMSCalculation();
            if (payemsCalculationHashMap.containsKey(payems.getDate())) {
                tempPAYEMSCalculation = payemsCalculationHashMap.get(payems.getDate());
            } else {
                tempPAYEMSCalculation.setToDate(payems.getDate());
            }

            while (queueItr.hasNext()) {
                PAYEMS payemsVal = queueItr.next();
                rollingAvg += payemsVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            payems.setRollAverageFlag(true);
            tempPAYEMSCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            payemsCalculationList.add(tempPAYEMSCalculation);

        }

        payemsCalculationReference = payemsCalculationRepository.saveAll(payemsCalculationList);
        payemsList = payemsRepostiory.saveAll(payemsList);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<PAYEMS> getLatestPAYEMSRecords() {

        if (NumberUtils.INTEGER_ZERO.equals(payemsCalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestPAYEMSRecords");
        Optional<PAYEMS> lastRecordOpt = payemsRepostiory.findTopByOrderByDateDesc();
        List<PAYEMS> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            PAYEMS lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "PAYEMS" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<PAYEMS> PAYEMSList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    PAYEMSList.add(new PAYEMS(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (PAYEMSList.size() > 1) { // As last record is already present in DB
                PAYEMSList.remove(0);
                response = payemsRepostiory.saveAll(PAYEMSList);
                logger.info("New record inserted in PAYEMS");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignPAYEMS() {
        List<PAYEMSCalculation> payemsCalculationList = payemsCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        PAYEMSCalculation lastUpdatedRecord = payemsCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(payemsCalculationList, new FederalEmploymentService.SortByDatePAYEMSCalculation());

        if(payemsCalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (PAYEMSCalculation payemsCalculation : payemsCalculationList) {
            if(payemsCalculation.getRoc() < lastRoc){
                payemsCalculation.setRocChangeSign(-1);
            }else if (payemsCalculation.getRoc() > lastRoc){
                payemsCalculation.setRocChangeSign(1);
            }else if(payemsCalculation.getRoc() == lastRoc){
                payemsCalculation.setRocChangeSign(0);
            }

            lastRoc = payemsCalculation.getRoc();
        }

        payemsCalculationRepository.saveAll(payemsCalculationList);
    }


}
