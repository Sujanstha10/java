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

import com.automation.trading.domain.fred.interestrates.DGS30;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.utility.RestUtility;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.DGS30Calculation;
import com.automation.trading.repository.DGS30CalculationRepository;
import com.automation.trading.repository.DGS30Repository;
import com.automation.trading.service.DGS30Service;

@Service
public class Dgs30UpdateService {

    @Autowired
    private DGS30Repository dgs30Repostiory;

    @Autowired
    private DGS30CalculationRepository dgs30CalculationRepository;

    @Autowired
    private DGS30Service dgs30RateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(Dgs30UpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs30CalculationRepository.findAny())) {

            dgs30RateOfChangeService.calculateRoc();
            dgs30RateOfChangeService.updateRocChangeSign();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<DGS30>> dgs30ListOpt = dgs30Repostiory.findByRocFlagIsFalseOrderByDate();
        Optional<DGS30> prevDGS30Opt = dgs30Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, DGS30Calculation> dgs30CalculationHashMap = new HashMap<>();
        List<DGS30> dgs30List = new ArrayList<>();

        if (dgs30ListOpt.isPresent()) {
            dgs30List = dgs30ListOpt.get();
            if (prevDGS30Opt.isPresent()) {
                dgs30List.add(prevDGS30Opt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs30List, new FederalReserveService.SortByDateDGS30());
        List<DGS30Calculation> dgs30CalculationReference = dgs30CalculationRepository.findAll();
        List<DGS30Calculation> dgs30CalculationModified = new ArrayList<>();
        Queue<DGS30> dgs30Queue = new LinkedList<>();

        for (DGS30Calculation dgs30Calculation : dgs30CalculationReference) {
            dgs30CalculationHashMap.put(dgs30Calculation.getToDate(), dgs30Calculation);
        }

        for (DGS30 dgs30 : dgs30List) {
            DGS30Calculation tempDGS30Calculation = new DGS30Calculation();

            if (dgs30Queue.size() == 2) {
                dgs30Queue.poll();
            }
            dgs30Queue.add(dgs30);

            if (dgs30.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<DGS30> queueIterator = dgs30Queue.iterator();

            if (dgs30CalculationHashMap.containsKey(dgs30.getDate())) {
                tempDGS30Calculation = dgs30CalculationHashMap.get(dgs30.getDate());
            } else {
                tempDGS30Calculation.setToDate(dgs30.getDate());
            }

            while (queueIterator.hasNext()) {
                DGS30 temp = queueIterator.next();
                temp.setRocFlag(true);
                if (dgs30Queue.size() == 1) {
                    roc = 0f;
                    tempDGS30Calculation.setRoc(roc);
                    tempDGS30Calculation.setToDate(dgs30.getDate());
                    tempDGS30Calculation.setRocChangeSign(0);
                } else {
                    roc = (dgs30.getValue() / ((LinkedList<DGS30>) dgs30Queue).get(0).getValue()) - 1;
                    tempDGS30Calculation.setRoc(roc);
                    tempDGS30Calculation.setToDate(dgs30.getDate());
                }

            }

            dgs30CalculationModified.add(tempDGS30Calculation);
        }

        dgs30List = dgs30Repostiory.saveAll(dgs30List);
        dgs30CalculationModified = dgs30CalculationRepository.saveAll(dgs30CalculationModified);
        logger.debug("Added new DGS30 row, " + dgs30CalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs30CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<DGS30Calculation>> dgs30CalculationListOpt = dgs30CalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<DGS30Calculation>> prevDGS30CalculationListOpt = dgs30CalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<DGS30Calculation> dgs30CalculationList = new ArrayList<>();

        if (dgs30CalculationListOpt.isPresent()) {
            dgs30CalculationList = dgs30CalculationListOpt.get();
            if (prevDGS30CalculationListOpt.isPresent()) {
                dgs30CalculationList.addAll(prevDGS30CalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs30CalculationList, new FederalReserveService.SortByDateDGS30Calculation());

        Queue<DGS30Calculation> dgs30CalculationPriorityQueue = new LinkedList<DGS30Calculation>();
        for (DGS30Calculation dgs30Calculation : dgs30CalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (dgs30CalculationPriorityQueue.size() == 4) {
                dgs30CalculationPriorityQueue.poll();
            }
            dgs30CalculationPriorityQueue.add(dgs30Calculation);

            if (dgs30Calculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<DGS30Calculation> queueIterator = dgs30CalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                DGS30Calculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            dgs30Calculation.setRocAnnRollAvgFlag(true);
            dgs30Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(dgs30CalculationList);
        dgs30CalculationList = dgs30CalculationRepository.saveAll(dgs30CalculationList);
        logger.info("New dgs30 calculation record inserted" + dgs30CalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month DGS30
     *
     * @return DGS30Calculation , updated DGS30Calculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs30CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<DGS30Calculation> dgs30CalculationList = new ArrayList<>();
        Optional<List<DGS30>> dgs30ListOpt = dgs30Repostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<DGS30>> prevDGS30ListOpt = dgs30Repostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<DGS30Calculation> dgs30CalculationReference = dgs30CalculationRepository.findAll();
        HashMap<Date, DGS30Calculation> dgs30CalculationHashMap = new HashMap<>();
        List<DGS30> dgs30List = new ArrayList<>();

        for (DGS30Calculation dgs30Calculation : dgs30CalculationReference) {
            dgs30CalculationHashMap.put(dgs30Calculation.getToDate(), dgs30Calculation);
        }

        Queue<DGS30> dgs30Queue = new LinkedList<>();

        if (dgs30ListOpt.isPresent()) {
            dgs30List = dgs30ListOpt.get();
            if (prevDGS30ListOpt.isPresent()) {
                dgs30List.addAll(prevDGS30ListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs30List, new FederalReserveService.SortByDateDGS30());

        for (DGS30 dgs30 : dgs30List) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (dgs30Queue.size() == 3) {
                dgs30Queue.poll();
            }
            dgs30Queue.add(dgs30);
            if (dgs30.getRollAverageFlag()) {
                continue;
            }

            Iterator<DGS30> queueItr = dgs30Queue.iterator();

            DGS30Calculation tempDGS30Calculation = new DGS30Calculation();
            if (dgs30CalculationHashMap.containsKey(dgs30.getDate())) {
                tempDGS30Calculation = dgs30CalculationHashMap.get(dgs30.getDate());
            } else {
                tempDGS30Calculation.setToDate(dgs30.getDate());
            }

            while (queueItr.hasNext()) {
                DGS30 dgs30Val = queueItr.next();
                rollingAvg += dgs30Val.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            dgs30.setRollAverageFlag(true);
            tempDGS30Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            dgs30CalculationList.add(tempDGS30Calculation);

        }

        dgs30CalculationReference = dgs30CalculationRepository.saveAll(dgs30CalculationList);
        dgs30List = dgs30Repostiory.saveAll(dgs30List);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<DGS30> getLatestDGS30Records() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs30CalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestDGS30Records");
        Optional<DGS30> lastRecordOpt = dgs30Repostiory.findTopByOrderByDateDesc();
        List<DGS30> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            DGS30 lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DGS30" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<DGS30> DGS30List = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    DGS30List.add(new DGS30(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (DGS30List.size() > 1) { // As last record is already present in DB
                DGS30List.remove(0);
                response = dgs30Repostiory.saveAll(DGS30List);
                logger.info("New record inserted in DGS30");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignDgs30() {
        List<DGS30Calculation> dgs30CalculationList = dgs30CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        DGS30Calculation lastUpdatedRecord = dgs30CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(dgs30CalculationList, new FederalReserveService.SortByDateDGS30Calculation());

        if(dgs30CalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (DGS30Calculation dgs30Calculation : dgs30CalculationList) {
            if(dgs30Calculation.getRoc() < lastRoc){
                dgs30Calculation.setRocChangeSign(-1);
            }else if (dgs30Calculation.getRoc() > lastRoc){
                dgs30Calculation.setRocChangeSign(1);
            }else if(dgs30Calculation.getRoc() == lastRoc){
                dgs30Calculation.setRocChangeSign(0);
            }

            lastRoc = dgs30Calculation.getRoc();
        }

        dgs30CalculationRepository.saveAll(dgs30CalculationList);
    }


}
