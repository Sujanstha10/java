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
import com.automation.trading.domain.calculation.DGS5Calculation;
import com.automation.trading.domain.fred.interestrates.DGS5;
import com.automation.trading.repository.DGS5CalculationRepository;
import com.automation.trading.repository.DGS5Repository;
import com.automation.trading.service.DGS5Service;
import com.automation.trading.service.FederalInterestRateService;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.utility.RestUtility;

@Service
public class Dgs5UpdateService {

    @Autowired
    private DGS5Repository dgs5Repostiory;

    @Autowired
    private DGS5CalculationRepository dgs5CalculationRepository;

    @Autowired
    private DGS5Service dgs5RateOfChangeService;

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


    private Logger logger = LoggerFactory.getLogger(Dgs5UpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs5CalculationRepository.findAny())) {
            dgs5RateOfChangeService.calculateRoc();
            dgs5RateOfChangeService.updateRocChangeSignDgs5();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<DGS5>> dgs5ListOpt = dgs5Repostiory.findByRocFlagIsFalseOrderByDate();
        Optional<DGS5> prevDGS5Opt = dgs5Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, DGS5Calculation> dgs5CalculationHashMap = new HashMap<>();

        List<DGS5> dgs5List = new ArrayList<>();

        if (dgs5ListOpt.isPresent()) {
            dgs5List = dgs5ListOpt.get();
            if (prevDGS5Opt.isPresent()) {
                dgs5List.add(prevDGS5Opt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs5List, new FederalInterestRateService.SortByDateDGS5());
        List<DGS5Calculation> dgs5CalculationReference = dgs5CalculationRepository.findAll();
        List<DGS5Calculation> dgs5CalculationModified = new ArrayList<>();
        Queue<DGS5> dgs5Queue = new LinkedList<>();

        for (DGS5Calculation dgs5Calculation : dgs5CalculationReference) {
            dgs5CalculationHashMap.put(dgs5Calculation.getToDate(), dgs5Calculation);
        }

        for (DGS5 dgs5 : dgs5List) {

            DGS5Calculation tempDGS5Calculation = new DGS5Calculation();

            if (dgs5Queue.size() == 2) {
                dgs5Queue.poll();
            }
            dgs5Queue.add(dgs5);

            if (dgs5.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<DGS5> queueIterator = dgs5Queue.iterator();

            if (dgs5CalculationHashMap.containsKey(dgs5.getDate())) {
                tempDGS5Calculation = dgs5CalculationHashMap.get(dgs5.getDate());
            } else {
                tempDGS5Calculation.setToDate(dgs5.getDate());
            }

            while (queueIterator.hasNext()) {
                DGS5 temp = queueIterator.next();
                temp.setRocFlag(true);
                if (dgs5Queue.size() == 1) {
                    roc = 0f;
                    tempDGS5Calculation.setRoc(roc);
                    tempDGS5Calculation.setToDate(dgs5.getDate());
                    tempDGS5Calculation.setRocChangeSign(0);
                } else {
                    roc = (dgs5.getValue() / ((LinkedList<DGS5>) dgs5Queue).get(0).getValue()) - 1;
                    tempDGS5Calculation.setRoc(roc);
                    tempDGS5Calculation.setToDate(dgs5.getDate());
                }

            }

            dgs5CalculationModified.add(tempDGS5Calculation);
        }

        dgs5List = dgs5Repostiory.saveAll(dgs5List);
        dgs5CalculationModified = dgs5CalculationRepository.saveAll(dgs5CalculationModified);
        logger.debug("Added new DGS5 row, " + dgs5CalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs5CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<DGS5Calculation>> dgs5CalculationListOpt = dgs5CalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<DGS5Calculation>> prevDGS5CalculationListOpt = dgs5CalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<DGS5Calculation> dgs5CalculationList = new ArrayList<>();

        if (dgs5CalculationListOpt.isPresent()) {
            dgs5CalculationList = dgs5CalculationListOpt.get();
            if (prevDGS5CalculationListOpt.isPresent()) {
                dgs5CalculationList.addAll(prevDGS5CalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs5CalculationList, new FederalReserveService.SortByDateDGS5Calculation());

        Queue<DGS5Calculation> dgs5CalculationPriorityQueue = new LinkedList<DGS5Calculation>();
        for (DGS5Calculation dgs5Calculation : dgs5CalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (dgs5CalculationPriorityQueue.size() == 4) {
                dgs5CalculationPriorityQueue.poll();
            }
            dgs5CalculationPriorityQueue.add(dgs5Calculation);

            if (dgs5Calculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<DGS5Calculation> queueIterator = dgs5CalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                DGS5Calculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            dgs5Calculation.setRocAnnRollAvgFlag(true);
            dgs5Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(dgs5CalculationList);
        dgs5CalculationList = dgs5CalculationRepository.saveAll(dgs5CalculationList);
        logger.info("New dgs5 calculation record inserted" + dgs5CalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month DGS5
     *
     * @return DGS5Calculation , updated DGS5Calculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs5CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<DGS5Calculation> dgs5CalculationList = new ArrayList<>();
        Optional<List<DGS5>> dgs5ListOpt = dgs5Repostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<DGS5>> prevDGS5ListOpt = dgs5Repostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<DGS5Calculation> dgs5CalculationReference = dgs5CalculationRepository.findAll();
        HashMap<Date, DGS5Calculation> dgs5CalculationHashMap = new HashMap<>();
        List<DGS5> dgs5List = new ArrayList<>();

        for (DGS5Calculation dgs5Calculation : dgs5CalculationReference) {
            dgs5CalculationHashMap.put(dgs5Calculation.getToDate(), dgs5Calculation);
        }

        Queue<DGS5> dgs5Queue = new LinkedList<>();

        if (dgs5ListOpt.isPresent()) {
            dgs5List = dgs5ListOpt.get();
            if (prevDGS5ListOpt.isPresent()) {
                dgs5List.addAll(prevDGS5ListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs5List, new FederalInterestRateService.SortByDateDGS5());

        for (DGS5 dgs5 : dgs5List) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (dgs5Queue.size() == 3) {
                dgs5Queue.poll();
            }
            dgs5Queue.add(dgs5);
            if (dgs5.getRollAverageFlag()) {
                continue;
            }

            Iterator<DGS5> queueItr = dgs5Queue.iterator();

            DGS5Calculation tempDGS5Calculation = new DGS5Calculation();
            if (dgs5CalculationHashMap.containsKey(dgs5.getDate())) {
                tempDGS5Calculation = dgs5CalculationHashMap.get(dgs5.getDate());
            } else {
                tempDGS5Calculation.setToDate(dgs5.getDate());
            }

            while (queueItr.hasNext()) {
                DGS5 dgs5Val = queueItr.next();
                rollingAvg += dgs5Val.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            dgs5.setRollAverageFlag(true);
            tempDGS5Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            dgs5CalculationList.add(tempDGS5Calculation);

        }

        dgs5CalculationReference = dgs5CalculationRepository.saveAll(dgs5CalculationList);
        dgs5List = dgs5Repostiory.saveAll(dgs5List);
        return;
    }

    @Scheduled(fixedDelay = 1000 * 60)
    public List<DGS5> getLatestDGS5Records() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs5CalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestDGS5Records");
        Optional<DGS5> lastRecordOpt = dgs5Repostiory.findTopByOrderByDateDesc();
        List<DGS5> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            DGS5 lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DGS5" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<DGS5> DGS5List = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    DGS5List.add(new DGS5(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (DGS5List.size() > 1) { // As last record is already present in DB
                DGS5List.remove(0);
                response = dgs5Repostiory.saveAll(DGS5List);
                logger.info("New record inserted in DGS5");
            }

        }
        return response;
    }


    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignDgs5() {
        List<DGS5Calculation> dgs5CalculationList = dgs5CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        DGS5Calculation lastUpdatedRecord = dgs5CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(dgs5CalculationList, new FederalReserveService.SortByDateDGS5Calculation());

        if(dgs5CalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (DGS5Calculation dgs5Calculation : dgs5CalculationList) {
            if(dgs5Calculation.getRoc() < lastRoc){
                dgs5Calculation.setRocChangeSign(-1);
            }else if (dgs5Calculation.getRoc() > lastRoc){
                dgs5Calculation.setRocChangeSign(1);
            }else if(dgs5Calculation.getRoc() == lastRoc){
                dgs5Calculation.setRocChangeSign(0);
            }

            lastRoc = dgs5Calculation.getRoc();
        }

        dgs5CalculationRepository.saveAll(dgs5CalculationList);
    }

    private FederalResponse consumeResponse(String urlToFetch) {
        HashMap<String, String> apiKeyMap = new HashMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", MediaType.APPLICATION_JSON.toString());
        headers.add("Content-Type", MediaType.APPLICATION_JSON.toString());
        headers.add("Cache-Control", "no-cache");
        HttpEntity entity = new HttpEntity(apiKeyMap, headers);
        FederalResponse json = restTemplate.exchange(urlToFetch, HttpMethod.GET, entity, FederalResponse.class)
                .getBody();
        return json;
    }

}
