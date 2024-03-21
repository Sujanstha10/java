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

import com.automation.trading.domain.fred.interestrates.DGS10;
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
import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.repository.DGS10CalculationRepository;
import com.automation.trading.repository.DGS10Repository;
import com.automation.trading.service.DGS10Service;

@Service
public class Dgs10UpdateService {

    @Autowired
    private DGS10Repository dgs10Repostiory;

    @Autowired
    private DGS10CalculationRepository dgs10CalculationRepository;

    @Autowired
    private DGS10Service dgs10RateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(Dgs10UpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs10CalculationRepository.findAny())) {
            dgs10RateOfChangeService.calculateRoc();
            dgs10RateOfChangeService.updateRocChangeSignDgs10();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<DGS10>> dgs10ListOpt = dgs10Repostiory.findByRocFlagIsFalseOrderByDate();
        Optional<DGS10> prevDGS10Opt = dgs10Repostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, DGS10Calculation> dgs10CalculationHashMap = new HashMap<>();
        DGS10Calculation prevDGS10CalculationRow = new DGS10Calculation();

        List<DGS10> dgs10List = new ArrayList<>();

        if (dgs10ListOpt.isPresent()) {
            dgs10List = dgs10ListOpt.get();
            if (prevDGS10Opt.isPresent()) {
                dgs10List.add(prevDGS10Opt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs10List, new FederalInterestRateService.SortByDateDGS10());
        List<DGS10Calculation> dgs10CalculationReference = dgs10CalculationRepository.findAll();
        List<DGS10Calculation> dgs10CalculationModified = new ArrayList<>();
        Queue<DGS10> dgs10Queue = new LinkedList<>();

        for (DGS10Calculation dgs10Calculation : dgs10CalculationReference) {
            dgs10CalculationHashMap.put(dgs10Calculation.getToDate(), dgs10Calculation);
        }

        for (DGS10 dgs10 : dgs10List) {
            DGS10Calculation tempDGS10Calculation = new DGS10Calculation();

            if (dgs10Queue.size() == 2) {
                dgs10Queue.poll();
            }
            dgs10Queue.add(dgs10);

            if (dgs10.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<DGS10> queueIterator = dgs10Queue.iterator();

            if (dgs10CalculationHashMap.containsKey(dgs10.getDate())) {
                tempDGS10Calculation = dgs10CalculationHashMap.get(dgs10.getDate());
            } else {
                tempDGS10Calculation.setToDate(dgs10.getDate());
            }

            while (queueIterator.hasNext()) {
                DGS10 temp = queueIterator.next();
                temp.setRocFlag(true);
                if (dgs10Queue.size() == 1) {
                    roc = 0f;
                    tempDGS10Calculation.setRoc(roc);
                    tempDGS10Calculation.setToDate(dgs10.getDate());
                    tempDGS10Calculation.setRocChangeSign(0);
                } else {
                    roc = (dgs10.getValue() / ((LinkedList<DGS10>) dgs10Queue).get(0).getValue()) - 1;
                    tempDGS10Calculation.setRoc(roc);
                    tempDGS10Calculation.setToDate(dgs10.getDate());
                }

            }

            dgs10CalculationModified.add(tempDGS10Calculation);
        }

        dgs10List = dgs10Repostiory.saveAll(dgs10List);
        dgs10CalculationModified = dgs10CalculationRepository.saveAll(dgs10CalculationModified);
        logger.debug("Added new DGS10 row, " + dgs10CalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs10CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<DGS10Calculation>> dgs10CalculationListOpt = dgs10CalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<DGS10Calculation>> prevDGS10CalculationListOpt = dgs10CalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<DGS10Calculation> dgs10CalculationList = new ArrayList<>();

        if (dgs10CalculationListOpt.isPresent()) {
            dgs10CalculationList = dgs10CalculationListOpt.get();
            if (prevDGS10CalculationListOpt.isPresent()) {
                dgs10CalculationList.addAll(prevDGS10CalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs10CalculationList, new FederalReserveService.SortByDateDGS10Calculation());

        Queue<DGS10Calculation> dgs10CalculationPriorityQueue = new LinkedList<DGS10Calculation>();
        for (DGS10Calculation dgs10Calculation : dgs10CalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (dgs10CalculationPriorityQueue.size() == 4) {
                dgs10CalculationPriorityQueue.poll();
            }
            dgs10CalculationPriorityQueue.add(dgs10Calculation);

            if (dgs10Calculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<DGS10Calculation> queueIterator = dgs10CalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                DGS10Calculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            dgs10Calculation.setRocAnnRollAvgFlag(true);
            dgs10Calculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(dgs10CalculationList);
        dgs10CalculationList = dgs10CalculationRepository.saveAll(dgs10CalculationList);
        logger.info("New dgs10 calculation record inserted" + dgs10CalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month DGS10
     *
     * @return DGS10Calculation , updated DGS10Calculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs10CalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<DGS10Calculation> dgs10CalculationList = new ArrayList<>();
        Optional<List<DGS10>> dgs10ListOpt = dgs10Repostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<DGS10>> prevDGS10ListOpt = dgs10Repostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<DGS10Calculation> dgs10CalculationReference = dgs10CalculationRepository.findAll();
        HashMap<Date, DGS10Calculation> dgs10CalculationHashMap = new HashMap<>();
        List<DGS10> dgs10List = new ArrayList<>();

        for (DGS10Calculation dgs10Calculation : dgs10CalculationReference) {
            dgs10CalculationHashMap.put(dgs10Calculation.getToDate(), dgs10Calculation);
        }

        Queue<DGS10> dgs10Queue = new LinkedList<>();

        if (dgs10ListOpt.isPresent()) {
            dgs10List = dgs10ListOpt.get();
            if (prevDGS10ListOpt.isPresent()) {
                dgs10List.addAll(prevDGS10ListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(dgs10List, new FederalInterestRateService.SortByDateDGS10());

        for (DGS10 dgs10 : dgs10List) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (dgs10Queue.size() == 3) {
                dgs10Queue.poll();
            }
            dgs10Queue.add(dgs10);
            if (dgs10.getRollAverageFlag()) {
                continue;
            }

            Iterator<DGS10> queueItr = dgs10Queue.iterator();

            DGS10Calculation tempDGS10Calculation = new DGS10Calculation();
            if (dgs10CalculationHashMap.containsKey(dgs10.getDate())) {
                tempDGS10Calculation = dgs10CalculationHashMap.get(dgs10.getDate());
            } else {
                tempDGS10Calculation.setToDate(dgs10.getDate());
            }

            while (queueItr.hasNext()) {
                DGS10 dgs10Val = queueItr.next();
                rollingAvg += dgs10Val.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            dgs10.setRollAverageFlag(true);
            tempDGS10Calculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            dgs10CalculationList.add(tempDGS10Calculation);

        }

        dgs10CalculationReference = dgs10CalculationRepository.saveAll(dgs10CalculationList);
        dgs10List = dgs10Repostiory.saveAll(dgs10List);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<DGS10> getLatestDGS10Records() {

        if (NumberUtils.INTEGER_ZERO.equals(dgs10CalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestDGS10Records");
        Optional<DGS10> lastRecordOpt = dgs10Repostiory.findTopByOrderByDateDesc();
        List<DGS10> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            DGS10 lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DGS10" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<DGS10> DGS10List = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    DGS10List.add(new DGS10(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (DGS10List.size() > 1) { // As last record is already present in DB
                DGS10List.remove(0);
                response = dgs10Repostiory.saveAll(DGS10List);
                logger.info("New record inserted in DGS10");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignDgs10() {
        List<DGS10Calculation> dgs10CalculationList = dgs10CalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        DGS10Calculation lastUpdatedRecord = dgs10CalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(dgs10CalculationList, new FederalReserveService.SortByDateDGS10Calculation());

        if(dgs10CalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (DGS10Calculation dgs10Calculation : dgs10CalculationList) {
            if(dgs10Calculation.getRoc() < lastRoc){
                dgs10Calculation.setRocChangeSign(-1);
            }else if (dgs10Calculation.getRoc() > lastRoc){
                dgs10Calculation.setRocChangeSign(1);
            }else if(dgs10Calculation.getRoc() == lastRoc){
                dgs10Calculation.setRocChangeSign(0);
            }

            lastRoc = dgs10Calculation.getRoc();
        }

        dgs10CalculationRepository.saveAll(dgs10CalculationList);
    }


}
