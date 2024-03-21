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
import com.automation.trading.domain.calculation.NROUCalculation;
import com.automation.trading.domain.fred.NROU;
import com.automation.trading.repository.NROUCalculationRepository;
import com.automation.trading.repository.NROURepostiory;
import com.automation.trading.service.FederalEmploymentService.SortByDateNROU;
import com.automation.trading.service.FederalEmploymentService.SortByDateNROUCalculation;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.service.NROUService;

@Service
public class NROUUpdateService {

	@Autowired
    private NROURepostiory nrouRepostiory;

    @Autowired
    private NROUCalculationRepository nrouCalculationRepository;

    @Autowired
    private NROUService nrouRateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(NROUUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(nrouCalculationRepository.findAny())) {
            nrouRateOfChangeService.calculateRoc();
            nrouRateOfChangeService.updateRocChangeSign();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<NROU>> nrouListOpt = nrouRepostiory.findByRocFlagIsFalseOrderByDate();
        Optional<NROU> prevNROUOpt = nrouRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, NROUCalculation> nrouCalculationHashMap = new HashMap<>();
        NROUCalculation prevNROUCalculationRow = new NROUCalculation();

        List<NROU> nrouList = new ArrayList<>();

        if (nrouListOpt.isPresent()) {
            nrouList = nrouListOpt.get();
            if (prevNROUOpt.isPresent()) {
                nrouList.add(prevNROUOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nrouList, new SortByDateNROU());
        List<NROUCalculation> nrouCalculationReference = nrouCalculationRepository.findAll();
        List<NROUCalculation> nrouCalculationModified = new ArrayList<>();
        Queue<NROU> nrouQueue = new LinkedList<>();

        for (NROUCalculation nrouCalculation : nrouCalculationReference) {
            nrouCalculationHashMap.put(nrouCalculation.getToDate(), nrouCalculation);
        }

        for (NROU nrou : nrouList) {
            NROUCalculation tempNROUCalculation = new NROUCalculation();

            if (nrouQueue.size() == 2) {
                nrouQueue.poll();
            }
            nrouQueue.add(nrou);

            if (nrou.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<NROU> queueIterator = nrouQueue.iterator();

            if (nrouCalculationHashMap.containsKey(nrou.getDate())) {
                tempNROUCalculation = nrouCalculationHashMap.get(nrou.getDate());
            } else {
                tempNROUCalculation.setToDate(nrou.getDate());
            }

            while (queueIterator.hasNext()) {
                NROU temp = queueIterator.next();
                temp.setRocFlag(true);
                if (nrouQueue.size() == 1) {
                    roc = 0f;
                    tempNROUCalculation.setRoc(roc);
                    tempNROUCalculation.setToDate(nrou.getDate());
                    tempNROUCalculation.setRocChangeSign(0);
                } else {
                    roc = (nrou.getValue() / ((LinkedList<NROU>) nrouQueue).get(0).getValue()) - 1;
                    tempNROUCalculation.setRoc(roc);
                    tempNROUCalculation.setToDate(nrou.getDate());
                }

            }

            nrouCalculationModified.add(tempNROUCalculation);
        }

        nrouList = nrouRepostiory.saveAll(nrouList);
        nrouCalculationModified = nrouCalculationRepository.saveAll(nrouCalculationModified);
        logger.debug("Added new NROU row, " + nrouCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(nrouCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<NROUCalculation>> nrouCalculationListOpt = nrouCalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<NROUCalculation>> prevNROUCalculationListOpt = nrouCalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<NROUCalculation> nrouCalculationList = new ArrayList<>();

        if (nrouCalculationListOpt.isPresent()) {
            nrouCalculationList = nrouCalculationListOpt.get();
            if (prevNROUCalculationListOpt.isPresent()) {
                nrouCalculationList.addAll(prevNROUCalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nrouCalculationList, new SortByDateNROUCalculation());

        Queue<NROUCalculation> nrouCalculationPriorityQueue = new LinkedList<NROUCalculation>();
        for (NROUCalculation nrouCalculation : nrouCalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (nrouCalculationPriorityQueue.size() == 4) {
                nrouCalculationPriorityQueue.poll();
            }
            nrouCalculationPriorityQueue.add(nrouCalculation);

            if (nrouCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<NROUCalculation> queueIterator = nrouCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                NROUCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            nrouCalculation.setRocAnnRollAvgFlag(true);
            nrouCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(nrouCalculationList);
        nrouCalculationList = nrouCalculationRepository.saveAll(nrouCalculationList);
        logger.info("New nrou calculation record inserted" + nrouCalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month NROU
     *
     * @return NROUCalculation , updated NROUCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(nrouCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<NROUCalculation> nrouCalculationList = new ArrayList<>();
        Optional<List<NROU>> nrouListOpt = nrouRepostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<NROU>> prevNROUListOpt = nrouRepostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<NROUCalculation> nrouCalculationReference = nrouCalculationRepository.findAll();
        HashMap<Date, NROUCalculation> nrouCalculationHashMap = new HashMap<>();
        List<NROU> nrouList = new ArrayList<>();

        for (NROUCalculation nrouCalculation : nrouCalculationReference) {
            nrouCalculationHashMap.put(nrouCalculation.getToDate(), nrouCalculation);
        }

        Queue<NROU> nrouQueue = new LinkedList<>();

        if (nrouListOpt.isPresent()) {
            nrouList = nrouListOpt.get();
            if (prevNROUListOpt.isPresent()) {
                nrouList.addAll(prevNROUListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(nrouList, new SortByDateNROU());

        for (NROU nrou : nrouList) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (nrouQueue.size() == 3) {
                nrouQueue.poll();
            }
            nrouQueue.add(nrou);
            if (nrou.getRollAverageFlag()) {
                continue;
            }

            Iterator<NROU> queueItr = nrouQueue.iterator();

            NROUCalculation tempNROUCalculation = new NROUCalculation();
            if (nrouCalculationHashMap.containsKey(nrou.getDate())) {
                tempNROUCalculation = nrouCalculationHashMap.get(nrou.getDate());
            } else {
                tempNROUCalculation.setToDate(nrou.getDate());
            }

            while (queueItr.hasNext()) {
                NROU nrouVal = queueItr.next();
                rollingAvg += nrouVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            nrou.setRollAverageFlag(true);
            tempNROUCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            nrouCalculationList.add(tempNROUCalculation);

        }

        nrouCalculationReference = nrouCalculationRepository.saveAll(nrouCalculationList);
        nrouList = nrouRepostiory.saveAll(nrouList);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<NROU> getLatestNROURecords() {

        if (NumberUtils.INTEGER_ZERO.equals(nrouCalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestNROURecords");
        Optional<NROU> lastRecordOpt = nrouRepostiory.findTopByOrderByDateDesc();
        List<NROU> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            NROU lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "NROU" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<NROU> NROUList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    NROUList.add(new NROU(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (NROUList.size() > 1) { // As last record is already present in DB
                NROUList.remove(0);
                response = nrouRepostiory.saveAll(NROUList);
                logger.info("New record inserted in NROU");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignNROU() {
        List<NROUCalculation> nrouCalculationList = nrouCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        NROUCalculation lastUpdatedRecord = nrouCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(nrouCalculationList, new SortByDateNROUCalculation());

        if(nrouCalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (NROUCalculation nrouCalculation : nrouCalculationList) {
            if(nrouCalculation.getRoc() < lastRoc){
                nrouCalculation.setRocChangeSign(-1);
            }else if (nrouCalculation.getRoc() > lastRoc){
                nrouCalculation.setRocChangeSign(1);
            }else if(nrouCalculation.getRoc() == lastRoc){
                nrouCalculation.setRocChangeSign(0);
            }

            lastRoc = nrouCalculation.getRoc();
        }

        nrouCalculationRepository.saveAll(nrouCalculationList);
    }

}
