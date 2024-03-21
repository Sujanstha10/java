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

import com.automation.trading.domain.fred.Gdp;
import com.automation.trading.repository.GdpCalculationRepository;
import com.automation.trading.repository.GdpRepository;
import com.automation.trading.service.FederalReserveService;
import com.automation.trading.service.GdpRateOfChangeService;
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
import com.automation.trading.domain.calculation.GdpCalculation;

@Service
public class GdpUpdateService {

    @Autowired
    private GdpRepository gdpRepostiory;

    @Autowired
    private GdpCalculationRepository gdpCalculationRepository;

    @Autowired
    private GdpRateOfChangeService gdpRateOfChangeService;

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

    private Logger logger = LoggerFactory.getLogger(GdpUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc() {

        if (NumberUtils.INTEGER_ZERO.equals(gdpCalculationRepository.findAny())) {
            gdpRateOfChangeService.calculateRoc();
            gdpRateOfChangeService.updateRocChangeSignGdp();
        }

        System.out.println("calculateRocRollingAnnualAvg");

        Optional<List<Gdp>> gdpListOpt = gdpRepostiory.findByRocFlagIsFalseOrderByDate();
        Optional<Gdp> prevGdpOpt = gdpRepostiory.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, GdpCalculation> gdpCalculationHashMap = new HashMap<>();
        GdpCalculation prevGdpCalculationRow = new GdpCalculation();

        List<Gdp> gdpList = new ArrayList<>();

        if (gdpListOpt.isPresent()) {
            gdpList = gdpListOpt.get();
            if (prevGdpOpt.isPresent()) {
                gdpList.add(prevGdpOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(gdpList, new FederalReserveService.SortByDateGdp());
        List<GdpCalculation> gdpCalculationReference = gdpCalculationRepository.findAll();
        List<GdpCalculation> gdpCalculationModified = new ArrayList<>();
        Queue<Gdp> gdpQueue = new LinkedList<>();

        for (GdpCalculation gdpCalculation : gdpCalculationReference) {
            gdpCalculationHashMap.put(gdpCalculation.getToDate(), gdpCalculation);
        }

        for (Gdp gdp : gdpList) {
            GdpCalculation tempGdpCalculation = new GdpCalculation();

            if (gdpQueue.size() == 2) {
                gdpQueue.poll();
            }
            gdpQueue.add(gdp);

            if (gdp.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;

            Iterator<Gdp> queueIterator = gdpQueue.iterator();

            if (gdpCalculationHashMap.containsKey(gdp.getDate())) {
                tempGdpCalculation = gdpCalculationHashMap.get(gdp.getDate());
            } else {
                tempGdpCalculation.setToDate(gdp.getDate());
            }

            while (queueIterator.hasNext()) {
                Gdp temp = queueIterator.next();
                temp.setRocFlag(true);
                if (gdpQueue.size() == 1) {
                    roc = 0f;
                    tempGdpCalculation.setRoc(roc);
                    tempGdpCalculation.setToDate(gdp.getDate());
                    tempGdpCalculation.setRocChangeSign(0);
                } else {
                    roc = (gdp.getValue() / ((LinkedList<Gdp>) gdpQueue).get(0).getValue()) - 1;
                    tempGdpCalculation.setRoc(roc);
                    tempGdpCalculation.setToDate(gdp.getDate());
                }

            }

            gdpCalculationModified.add(tempGdpCalculation);
        }

        gdpList = gdpRepostiory.saveAll(gdpList);
        gdpCalculationModified = gdpCalculationRepository.saveAll(gdpCalculationModified);
        logger.debug("Added new Gdp row, " + gdpCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg() {

        if (NumberUtils.INTEGER_ZERO.equals(gdpCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRocRollingAnnualAvg");
        Optional<List<GdpCalculation>> gdpCalculationListOpt = gdpCalculationRepository
                .findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<GdpCalculation>> prevGdpCalculationListOpt = gdpCalculationRepository
                .findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<GdpCalculation> gdpCalculationList = new ArrayList<>();

        if (gdpCalculationListOpt.isPresent()) {
            gdpCalculationList = gdpCalculationListOpt.get();
            if (prevGdpCalculationListOpt.isPresent()) {
                gdpCalculationList.addAll(prevGdpCalculationListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(gdpCalculationList, new FederalReserveService.SortByDateGdpCalculation());

        Queue<GdpCalculation> gdpCalculationPriorityQueue = new LinkedList<GdpCalculation>();
        for (GdpCalculation gdpCalculation : gdpCalculationList) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (gdpCalculationPriorityQueue.size() == 4) {
                gdpCalculationPriorityQueue.poll();
            }
            gdpCalculationPriorityQueue.add(gdpCalculation);

            if (gdpCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<GdpCalculation> queueIterator = gdpCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                GdpCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            gdpCalculation.setRocAnnRollAvgFlag(true);
            gdpCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(gdpCalculationList);
        gdpCalculationList = gdpCalculationRepository.saveAll(gdpCalculationList);
        logger.info("New gdp calculation record inserted" + gdpCalculationList);
        return;

    }

    /**
     * Calculates Rolling Average of Three Month Gdp
     *
     * @return GdpCalculation , updated GdpCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth() {

        if (NumberUtils.INTEGER_ZERO.equals(gdpCalculationRepository.findAny())) {
            return;
        }

        System.out.println("calculateRollAvgThreeMonth");

        List<GdpCalculation> gdpCalculationList = new ArrayList<>();
        Optional<List<Gdp>> gdpListOpt = gdpRepostiory.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<Gdp>> prevGdpListOpt = gdpRepostiory
                .findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<GdpCalculation> gdpCalculationReference = gdpCalculationRepository.findAll();
        HashMap<Date, GdpCalculation> gdpCalculationHashMap = new HashMap<>();
        List<Gdp> gdpList = new ArrayList<>();

        for (GdpCalculation gdpCalculation : gdpCalculationReference) {
            gdpCalculationHashMap.put(gdpCalculation.getToDate(), gdpCalculation);
        }

        Queue<Gdp> gdpQueue = new LinkedList<>();

        if (gdpListOpt.isPresent()) {
            gdpList = gdpListOpt.get();
            if (prevGdpListOpt.isPresent()) {
                gdpList.addAll(prevGdpListOpt.get());
            }
        } else {
            return;
        }

        Collections.sort(gdpList, new FederalReserveService.SortByDateGdp());

        for (Gdp gdp : gdpList) {

            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            if (gdpQueue.size() == 3) {
                gdpQueue.poll();
            }
            gdpQueue.add(gdp);
            if (gdp.getRollAverageFlag()) {
                continue;
            }

            Iterator<Gdp> queueItr = gdpQueue.iterator();

            GdpCalculation tempGdpCalculation = new GdpCalculation();
            if (gdpCalculationHashMap.containsKey(gdp.getDate())) {
                tempGdpCalculation = gdpCalculationHashMap.get(gdp.getDate());
            } else {
                tempGdpCalculation.setToDate(gdp.getDate());
            }

            while (queueItr.hasNext()) {
                Gdp gdpVal = queueItr.next();
                rollingAvg += gdpVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            gdp.setRollAverageFlag(true);
            tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            gdpCalculationList.add(tempGdpCalculation);

        }

        gdpCalculationReference = gdpCalculationRepository.saveAll(gdpCalculationList);
        gdpList = gdpRepostiory.saveAll(gdpList);
        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<Gdp> getLatestGdpRecords() {

        if (NumberUtils.INTEGER_ZERO.equals(gdpCalculationRepository.findAny())) {
            return null;
        }
        System.out.println("getLatestGdpRecords");
        Optional<Gdp> lastRecordOpt = gdpRepostiory.findTopByOrderByDateDesc();
        List<Gdp> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            Gdp lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "Gdp" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<Gdp> GdpList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    GdpList.add(new Gdp(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (GdpList.size() > 1) { // As last record is already present in DB
                GdpList.remove(0);
                response = gdpRepostiory.saveAll(GdpList);
                logger.info("New record inserted in Gdp");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignDgs10() {
        List<GdpCalculation> gdpCalculationList = gdpCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        GdpCalculation lastUpdatedRecord = gdpCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(gdpCalculationList, new FederalReserveService.SortByDateGdpCalculation());

        if(gdpCalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (GdpCalculation gdpCalculation : gdpCalculationList) {
            if(gdpCalculation.getRoc() < lastRoc){
                gdpCalculation.setRocChangeSign(-1);
            }else if (gdpCalculation.getRoc() > lastRoc){
                gdpCalculation.setRocChangeSign(1);
            }else if(gdpCalculation.getRoc() == lastRoc){
                gdpCalculation.setRocChangeSign(0);
            }

            lastRoc = gdpCalculation.getRoc();
        }

        gdpCalculationRepository.saveAll(gdpCalculationList);
    }



}
