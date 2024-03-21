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

import com.automation.trading.domain.fred.MANEMP;
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
import com.automation.trading.domain.calculation.MANEMPCalculation;
import com.automation.trading.repository.MANEMPCalculationRepository;
import com.automation.trading.repository.MANEMPRepository;
import com.automation.trading.service.MANEMPService;

@Service
public class MANEMPUpdateService {

    @Autowired
    private MANEMPRepository manempRepostiory;

    @Autowired
    private MANEMPCalculationRepository manempCalculationRepository;

    @Autowired
    private MANEMPService manempRateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(MANEMPUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(manempCalculationRepository.findAny())) {
            manempRateOfChangeService.calculateRoc();
            manempRateOfChangeService.updateRocChangeSignMANEMP();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<MANEMP>> manempListOpt = manempRepostiory.findByRocFlagIsFalseOrderByDate();
        Optional<MANEMP> prevMANEMPOpt = manempRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, MANEMPCalculation> manempCalculationHashMap = new HashMap<>();
        MANEMPCalculation prevMANEMPCalculationRow = new MANEMPCalculation();

        List<MANEMP> manempList = new ArrayList<>();

        if (manempListOpt.isPresent()) {
            manempList = manempListOpt.get();
            if (prevMANEMPOpt.isPresent()) {
                manempList.add(prevMANEMPOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(manempList, new FederalEmploymentService.SortByDateMANEMP());
        List<MANEMPCalculation> manempCalculationReference = manempCalculationRepository.findAll();
        List<MANEMPCalculation> manempCalculationModified = new ArrayList<>();
        Queue<MANEMP> manempQueue = new LinkedList<>();

        for (MANEMPCalculation manempCalculation : manempCalculationReference) {
            manempCalculationHashMap.put(manempCalculation.getToDate(), manempCalculation);
        }

        for (MANEMP manemp : manempList) {
            MANEMPCalculation tempMANEMPCalculation = new MANEMPCalculation();

            if (manempQueue.size() == 2) {
                manempQueue.poll();
            }
            manempQueue.add(manemp);

            if (manemp.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<MANEMP> queueIterator = manempQueue.iterator();

            if (manempCalculationHashMap.containsKey(manemp.getDate())) {
                tempMANEMPCalculation = manempCalculationHashMap.get(manemp.getDate());
            } else {
                tempMANEMPCalculation.setToDate(manemp.getDate());
            }

            while (queueIterator.hasNext()) {
                MANEMP temp = queueIterator.next();
                temp.setRocFlag(true);
                if (manempQueue.size() == 1) {
                    roc = 0f;
                    tempMANEMPCalculation.setRoc(roc);
                    tempMANEMPCalculation.setToDate(manemp.getDate());
                    tempMANEMPCalculation.setRocChangeSign(0);
                } else {
                    roc = (manemp.getValue() / ((LinkedList<MANEMP>) manempQueue).get(0).getValue()) - 1;
                    tempMANEMPCalculation.setRoc(roc);
                    tempMANEMPCalculation.setToDate(manemp.getDate());
                }

            }

            manempCalculationModified.add(tempMANEMPCalculation);
        }

        manempList = manempRepostiory.saveAll(manempList);
        manempCalculationModified = manempCalculationRepository.saveAll(manempCalculationModified);
        logger.debug("Added new MANEMP row, " + manempCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(manempCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<MANEMPCalculation>> manempCalculationListOpt = manempCalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<MANEMPCalculation>> prevMANEMPCalculationListOpt = manempCalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<MANEMPCalculation> manempCalculationList = new ArrayList<>();

        if (manempCalculationListOpt.isPresent()) {
            manempCalculationList = manempCalculationListOpt.get();
            if (prevMANEMPCalculationListOpt.isPresent()) {
                manempCalculationList.addAll(prevMANEMPCalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(manempCalculationList, new FederalEmploymentService.SortByDateMANEMPCalculation());

        Queue<MANEMPCalculation> manempCalculationPriorityQueue = new LinkedList<MANEMPCalculation>();
        for (MANEMPCalculation manempCalculation : manempCalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (manempCalculationPriorityQueue.size() == 4) {
                manempCalculationPriorityQueue.poll();
            }
            manempCalculationPriorityQueue.add(manempCalculation);

            if (manempCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<MANEMPCalculation> queueIterator = manempCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                MANEMPCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            manempCalculation.setRocAnnRollAvgFlag(true);
            manempCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(manempCalculationList);
        manempCalculationList = manempCalculationRepository.saveAll(manempCalculationList);
        logger.info("New manemp calculation record inserted" + manempCalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month MANEMP
     *
     * @return MANEMPCalculation , updated MANEMPCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(manempCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<MANEMPCalculation> manempCalculationList = new ArrayList<>();
        Optional<List<MANEMP>> manempListOpt = manempRepostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<MANEMP>> prevMANEMPListOpt = manempRepostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<MANEMPCalculation> manempCalculationReference = manempCalculationRepository.findAll();
        HashMap<Date, MANEMPCalculation> manempCalculationHashMap = new HashMap<>();
        List<MANEMP> manempList = new ArrayList<>();

        for (MANEMPCalculation manempCalculation : manempCalculationReference) {
            manempCalculationHashMap.put(manempCalculation.getToDate(), manempCalculation);
        }

        Queue<MANEMP> manempQueue = new LinkedList<>();

        if (manempListOpt.isPresent()) {
            manempList = manempListOpt.get();
            if (prevMANEMPListOpt.isPresent()) {
                manempList.addAll(prevMANEMPListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(manempList, new FederalEmploymentService.SortByDateMANEMP());

        for (MANEMP manemp : manempList) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (manempQueue.size() == 3) {
                manempQueue.poll();
            }
            manempQueue.add(manemp);
            if (manemp.getRollAverageFlag()) {
                continue;
            }

            Iterator<MANEMP> queueItr = manempQueue.iterator();

            MANEMPCalculation tempMANEMPCalculation = new MANEMPCalculation();
            if (manempCalculationHashMap.containsKey(manemp.getDate())) {
                tempMANEMPCalculation = manempCalculationHashMap.get(manemp.getDate());
            } else {
                tempMANEMPCalculation.setToDate(manemp.getDate());
            }

            while (queueItr.hasNext()) {
                MANEMP manempVal = queueItr.next();
                rollingAvg += manempVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            manemp.setRollAverageFlag(true);
            tempMANEMPCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            manempCalculationList.add(tempMANEMPCalculation);

        }

        manempCalculationReference = manempCalculationRepository.saveAll(manempCalculationList);
        manempList = manempRepostiory.saveAll(manempList);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<MANEMP> getLatestMANEMPRecords() {

        if (NumberUtils.INTEGER_ZERO.equals(manempCalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestMANEMPRecords");
        Optional<MANEMP> lastRecordOpt = manempRepostiory.findTopByOrderByDateDesc();
        List<MANEMP> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            MANEMP lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "MANEMP" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<MANEMP> MANEMPList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    MANEMPList.add(new MANEMP(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (MANEMPList.size() > 1) { // As last record is already present in DB
                MANEMPList.remove(0);
                response = manempRepostiory.saveAll(MANEMPList);
                logger.info("New record inserted in MANEMP");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignMANEMP() {
        List<MANEMPCalculation> manempCalculationList = manempCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        MANEMPCalculation lastUpdatedRecord = manempCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(manempCalculationList, new FederalEmploymentService.SortByDateMANEMPCalculation());

        if(manempCalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (MANEMPCalculation manempCalculation : manempCalculationList) {
            if(manempCalculation.getRoc() < lastRoc){
                manempCalculation.setRocChangeSign(-1);
            }else if (manempCalculation.getRoc() > lastRoc){
                manempCalculation.setRocChangeSign(1);
            }else if(manempCalculation.getRoc() == lastRoc){
                manempCalculation.setRocChangeSign(0);
            }

            lastRoc = manempCalculation.getRoc();
        }

        manempCalculationRepository.saveAll(manempCalculationList);
    }


}
