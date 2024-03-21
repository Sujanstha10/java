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
import com.automation.trading.domain.calculation.NROUSTCalculation;
import com.automation.trading.domain.fred.NROUST;
import com.automation.trading.repository.NROUSTCalculationRepository;
import com.automation.trading.repository.NROUSTRepository;
import com.automation.trading.service.FederalEmploymentService.SortByDateNROUST;
import com.automation.trading.service.FederalEmploymentService.SortByDateNROUSTCalculation;
import com.automation.trading.service.NROUSTService;
@Service
public class NROUSTUpdateService {

    @Autowired
    private NROUSTRepository nroustRepostiory;

    @Autowired
    private NROUSTCalculationRepository nroustCalculationRepository;

    @Autowired
    private NROUSTService nroustRateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(NROUSTUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(nroustCalculationRepository.findAny())) {
            nroustRateOfChangeService.calculateRoc();
            nroustRateOfChangeService.updateRocChangeSignNROUST();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<NROUST>> nroustListOpt = nroustRepostiory.findByRocFlagIsFalseOrderByDate();
        Optional<NROUST> prevNROUSTOpt = nroustRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, NROUSTCalculation> nroustCalculationHashMap = new HashMap<>();
        NROUSTCalculation prevNROUSTCalculationRow = new NROUSTCalculation();

        List<NROUST> nroustList = new ArrayList<>();

        if (nroustListOpt.isPresent()) {
            nroustList = nroustListOpt.get();
            if (prevNROUSTOpt.isPresent()) {
                nroustList.add(prevNROUSTOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nroustList, new SortByDateNROUST());
        List<NROUSTCalculation> nroustCalculationReference = nroustCalculationRepository.findAll();
        List<NROUSTCalculation> nroustCalculationModified = new ArrayList<>();
        Queue<NROUST> nroustQueue = new LinkedList<>();

        for (NROUSTCalculation nroustCalculation : nroustCalculationReference) {
            nroustCalculationHashMap.put(nroustCalculation.getToDate(), nroustCalculation);
        }

        for (NROUST nroust : nroustList) {
            NROUSTCalculation tempNROUSTCalculation = new NROUSTCalculation();

            if (nroustQueue.size() == 2) {
                nroustQueue.poll();
            }
            nroustQueue.add(nroust);

            if (nroust.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<NROUST> queueIterator = nroustQueue.iterator();

            if (nroustCalculationHashMap.containsKey(nroust.getDate())) {
                tempNROUSTCalculation = nroustCalculationHashMap.get(nroust.getDate());
            } else {
                tempNROUSTCalculation.setToDate(nroust.getDate());
            }

            while (queueIterator.hasNext()) {
                NROUST temp = queueIterator.next();
                temp.setRocFlag(true);
                if (nroustQueue.size() == 1) {
                    roc = 0f;
                    tempNROUSTCalculation.setRoc(roc);
                    tempNROUSTCalculation.setToDate(nroust.getDate());
                    tempNROUSTCalculation.setRocChangeSign(0);
                } else {
                    roc = (nroust.getValue() / ((LinkedList<NROUST>) nroustQueue).get(0).getValue()) - 1;
                    tempNROUSTCalculation.setRoc(roc);
                    tempNROUSTCalculation.setToDate(nroust.getDate());
                }

            }

            nroustCalculationModified.add(tempNROUSTCalculation);
        }

        nroustList = nroustRepostiory.saveAll(nroustList);
        nroustCalculationModified = nroustCalculationRepository.saveAll(nroustCalculationModified);
        logger.debug("Added new NROUST row, " + nroustCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(nroustCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<NROUSTCalculation>> nroustCalculationListOpt = nroustCalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<NROUSTCalculation>> prevNROUSTCalculationListOpt = nroustCalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<NROUSTCalculation> nroustCalculationList = new ArrayList<>();

        if (nroustCalculationListOpt.isPresent()) {
            nroustCalculationList = nroustCalculationListOpt.get();
            if (prevNROUSTCalculationListOpt.isPresent()) {
                nroustCalculationList.addAll(prevNROUSTCalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nroustCalculationList, new SortByDateNROUSTCalculation());

        Queue<NROUSTCalculation> nroustCalculationPriorityQueue = new LinkedList<NROUSTCalculation>();
        for (NROUSTCalculation nroustCalculation : nroustCalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (nroustCalculationPriorityQueue.size() == 4) {
                nroustCalculationPriorityQueue.poll();
            }
            nroustCalculationPriorityQueue.add(nroustCalculation);

            if (nroustCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<NROUSTCalculation> queueIterator = nroustCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                NROUSTCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            nroustCalculation.setRocAnnRollAvgFlag(true);
            nroustCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(nroustCalculationList);
        nroustCalculationList = nroustCalculationRepository.saveAll(nroustCalculationList);
        logger.info("New nroust calculation record inserted" + nroustCalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month NROUST
     *
     * @return NROUSTCalculation , updated NROUSTCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(nroustCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<NROUSTCalculation> nroustCalculationList = new ArrayList<>();
        Optional<List<NROUST>> nroustListOpt = nroustRepostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<NROUST>> prevNROUSTListOpt = nroustRepostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<NROUSTCalculation> nroustCalculationReference = nroustCalculationRepository.findAll();
        HashMap<Date, NROUSTCalculation> nroustCalculationHashMap = new HashMap<>();
        List<NROUST> nroustList = new ArrayList<>();

        for (NROUSTCalculation nroustCalculation : nroustCalculationReference) {
            nroustCalculationHashMap.put(nroustCalculation.getToDate(), nroustCalculation);
        }

        Queue<NROUST> nroustQueue = new LinkedList<>();

        if (nroustListOpt.isPresent()) {
            nroustList = nroustListOpt.get();
            if (prevNROUSTListOpt.isPresent()) {
                nroustList.addAll(prevNROUSTListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nroustList, new SortByDateNROUST());

        for (NROUST nroust : nroustList) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (nroustQueue.size() == 3) {
                nroustQueue.poll();
            }
            nroustQueue.add(nroust);
            if (nroust.getRollAverageFlag()) {
                continue;
            }

            Iterator<NROUST> queueItr = nroustQueue.iterator();

            NROUSTCalculation tempNROUSTCalculation = new NROUSTCalculation();
            if (nroustCalculationHashMap.containsKey(nroust.getDate())) {
                tempNROUSTCalculation = nroustCalculationHashMap.get(nroust.getDate());
            } else {
                tempNROUSTCalculation.setToDate(nroust.getDate());
            }

            while (queueItr.hasNext()) {
                NROUST nroustVal = queueItr.next();
                rollingAvg += nroustVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            nroust.setRollAverageFlag(true);
            tempNROUSTCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            nroustCalculationList.add(tempNROUSTCalculation);

        }

        nroustCalculationReference = nroustCalculationRepository.saveAll(nroustCalculationList);
        nroustList = nroustRepostiory.saveAll(nroustList);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<NROUST> getLatestNROUSTRecords() {

        if (NumberUtils.INTEGER_ZERO.equals(nroustCalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestNROUSTRecords");
        Optional<NROUST> lastRecordOpt = nroustRepostiory.findTopByOrderByDateDesc();
        List<NROUST> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            NROUST lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "NROUST" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<NROUST> NROUSTList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    NROUSTList.add(new NROUST(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (NROUSTList.size() > 1) { // As last record is already present in DB
                NROUSTList.remove(0);
                response = nroustRepostiory.saveAll(NROUSTList);
                logger.info("New record inserted in NROUST");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignNROUST() {
        List<NROUSTCalculation> nroustCalculationList = nroustCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        NROUSTCalculation lastUpdatedRecord = nroustCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(nroustCalculationList, new SortByDateNROUSTCalculation());

        if (nroustCalculationList.size() == 0) {
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (NROUSTCalculation nroustCalculation : nroustCalculationList) {
            if (nroustCalculation.getRoc() < lastRoc) {
                nroustCalculation.setRocChangeSign(-1);
            } else if (nroustCalculation.getRoc() > lastRoc) {
                nroustCalculation.setRocChangeSign(1);
            } else if (nroustCalculation.getRoc() == lastRoc) {
                nroustCalculation.setRocChangeSign(0);
            }

            lastRoc = nroustCalculation.getRoc();
        }

        nroustCalculationRepository.saveAll(nroustCalculationList);
    }
}
