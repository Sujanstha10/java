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

import com.automation.trading.common.FederalResponse;
import com.automation.trading.domain.calculation.DffCalculation;
import com.automation.trading.service.DffRateOfChangeService;
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
import com.automation.trading.domain.fred.DFF;
import com.automation.trading.repository.DFFRepository;
import com.automation.trading.repository.DffCalculationRepository;
import com.automation.trading.service.FederalReserveService;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DffUpdateService {


    @Autowired
    DFFRepository dffRepository;
    @Autowired
    DffCalculationRepository dffCalculationRepository;
    @Autowired
    DffRateOfChangeService dffRateOfChangeService;
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

    private Logger logger = LoggerFactory.getLogger(DffUpdateService.class);

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRoc(){

        if (NumberUtils.INTEGER_ZERO.equals(dffCalculationRepository.findAny())) {
            dffRateOfChangeService.calculateRoc();
            dffRateOfChangeService.updateRocChangeSignDFF();
        }

        Optional<List<DFF>> dffListOpt = dffRepository.findByRocFlagIsFalseOrderByDate();
        Optional<DFF> prevDFFOpt =  dffRepository.findFirstByRocFlagIsTrueOrderByDateDesc();
        HashMap<Date, DffCalculation> dffCalculationHashMap = new HashMap<>();

        List<DFF> dffList = new ArrayList<>();

        if(dffListOpt.isPresent()){
            dffList = dffListOpt.get();
            if(prevDFFOpt.isPresent()){
                dffList.add(prevDFFOpt.get());
            }
        }else {
            return;
        }

        Collections.sort(dffList, new FederalReserveService.SortByDateDff());
        List<DffCalculation> dffCalculationReference = dffCalculationRepository.findAll();
        List<DffCalculation> dffCalculationModified = new ArrayList<>();
        Queue<DFF> dffQueue = new LinkedList<>();

        for(DffCalculation dffCalculation : dffCalculationReference){
            dffCalculationHashMap.put(dffCalculation.getToDate(),dffCalculation);
        }

        for(DFF dff : dffList){

            DffCalculation tempDffCalculation=new DffCalculation();

            if(dffQueue.size()==2){
                dffQueue.poll();
            }
            dffQueue.add(dff);

            if(dff.getRocFlag()){
                continue;
            }
            Float roc = 0.0f;

            Iterator<DFF> queueIterator = dffQueue.iterator();

            if(dffCalculationHashMap.containsKey(dff.getDate())){
                tempDffCalculation = dffCalculationHashMap.get(dff.getDate());
            }


            while (queueIterator.hasNext()){
                DFF temp = queueIterator.next();
                temp.setRocFlag(true);
                if(dffQueue.size()==1){
                    roc = 0f;
                    tempDffCalculation.setRoc(roc);
                    tempDffCalculation.setToDate(dff.getDate());
                    tempDffCalculation.setRocChangeSign(0);
                }else{
                    roc= (dff.getValue()/((LinkedList<DFF>) dffQueue).get(0).getValue())-1;
                    tempDffCalculation.setRoc(roc);
                    tempDffCalculation.setToDate(dff.getDate());
                }

            }

            dffCalculationModified.add(tempDffCalculation);
        }

        dffList =dffRepository.saveAll(dffList);
        dffCalculationModified =dffCalculationRepository.saveAll(dffCalculationModified);
        logger.debug("Added new Dff row, "+dffCalculationModified);

        return;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRocRollingAnnualAvg(){

        if (NumberUtils.INTEGER_ZERO.equals(dffCalculationRepository.findAny())) {
            return;
        }

        Optional<List<DffCalculation>> dffCalculationListOpt = dffCalculationRepository.findByRocAnnRollAvgFlagIsFalseAndRocIsNotNullOrderByToDate();
        Optional<List<DffCalculation>> prevDffCalculationListOpt = dffCalculationRepository.findTop3ByRocAnnRollAvgFlagIsTrueOrderByToDateDesc();
        List<DffCalculation> dffCalculationList = new ArrayList<>();

        if(dffCalculationListOpt.isPresent()){
            dffCalculationList = dffCalculationListOpt.get();
            if(prevDffCalculationListOpt.isPresent()){
                dffCalculationList.addAll(prevDffCalculationListOpt.get());
            }
        }else{
            return;
        }

        Collections.sort(dffCalculationList, new FederalReserveService.SortByDateDffCalculation());

        Queue<DffCalculation> dffCalculationPriorityQueue = new LinkedList<DffCalculation>();
        for(DffCalculation dffCalculation : dffCalculationList){
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg =0.0f;
            int period=0;
            if(dffCalculationPriorityQueue.size()==4){
                dffCalculationPriorityQueue.poll();
            }
            dffCalculationPriorityQueue.add(dffCalculation);

            if(dffCalculation.getRocAnnRollAvgFlag()){
                continue;
            }
            Iterator<DffCalculation> queueIterator = dffCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()){
                DffCalculation temp = queueIterator.next();
                rocFourMonth+=temp.getRoc();
                period++;
            }
            rocFourMonthAvg =  rocFourMonth/period;
            dffCalculation.setRocAnnRollAvgFlag(true);
            dffCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(dffCalculationList);
        dffCalculationList =dffCalculationRepository.saveAll(dffCalculationList);
        logger.info("New dff calculation record inserted"+ dffCalculationList);
        return;

    }


    /**
     * Calculates Rolling Average of Three Month DFF
     * @return DffCalculation , updated DffCalculation Table
     */
    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void calculateRollAvgThreeMonth(){

        if (NumberUtils.INTEGER_ZERO.equals(dffCalculationRepository.findAny())) {
            return;
        }

        List<DffCalculation> dffCalculationList=new ArrayList<>();
        Optional<List<DFF>> dffListOpt = dffRepository.findByRollAverageFlagIsFalseOrderByDate();
        Optional<List<DFF>> prevDffListOpt = dffRepository.findTop2ByRollAverageFlagIsTrueOrderByDateDesc();
        List<DffCalculation> dffCalculationReference = dffCalculationRepository.findAll();
        HashMap<Date, DffCalculation> dffCalculationHashMap = new HashMap<>();
        List<DFF> dffList = new ArrayList<>();

        for(DffCalculation dffCalculation : dffCalculationReference){
            dffCalculationHashMap.put(dffCalculation.getToDate(),dffCalculation);
        }


        Queue<DFF> dffQueue = new LinkedList<>();

        if(dffListOpt.isPresent()){
            dffList = dffListOpt.get();
            if(prevDffListOpt.isPresent()){
                dffList.addAll(prevDffListOpt.get());
            }
        }else{
            return ;
        }

        Collections.sort(dffList, new FederalReserveService.SortByDateDff());

        for (DFF dff : dffList){

            Float rollingAvg =0.0f;
            Float rollingAvgThreeMon =0f;
            int period =0;

            if(dffQueue.size()==3){
                dffQueue.poll();
            }
            dffQueue.add(dff);
            if(dff.getRollAverageFlag()){
                continue;
            }

            Iterator<DFF> queueItr = dffQueue.iterator();

            DffCalculation tempDffCalculation = new DffCalculation();
            if(dffCalculationHashMap.containsKey(dff.getDate())){
                tempDffCalculation = dffCalculationHashMap.get(dff.getDate());
            }


            while (queueItr.hasNext()){
                DFF dffVal = queueItr.next();
                rollingAvg +=dffVal.getValue();
                period++;
            }

            rollingAvgThreeMon =  rollingAvg/period;

            dff.setRollAverageFlag(true);
            tempDffCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            dffCalculationList.add(tempDffCalculation);

        }

        dffCalculationReference = dffCalculationRepository.saveAll(dffCalculationList);
        dffList = dffRepository.saveAll(dffList);
        return ;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public List<DFF> getLatestDFFRecords() {
        Optional<DFF> lastRecordOpt = dffRepository.findTopByOrderByDateDesc();
        List<DFF> response = new ArrayList<>();
        if (lastRecordOpt.isPresent()) {
            DFF lastRecord = lastRecordOpt.get();
            String lastDate = lastRecord.getDate().toString();
            String transactionUrl = QUANDL_HOST_URL + "FRED" + "/" + "DFF" + "/" + QUANDL_DATA_FORMAT;

            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(transactionUrl)
                    // Add query parameter
                    .queryParam("start_date", lastDate).queryParam("order", "ASC")
                    .queryParam(QUANDL_API_KEY_NAME, QUANDL_API_KEY_VALUE);

            List<DFF> dffList = new ArrayList<>();
            FederalResponse json = restUtility.consumeResponse(builder.toUriString());
            json.getDataset_data().getData().stream().forEach(o -> {
                ArrayList temp = (ArrayList) o;
                try {
                    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(temp.get(0).toString());
                    dffList.add(new DFF(date, Float.parseFloat(temp.get(1).toString())));
                    ;
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            });

            if (dffList.size() > 1) { // As last record is already present in DB
                dffList.remove(0);
                response = dffRepository.saveAll(dffList);
                logger.info("New record inserted in GDP");
            }

        }
        return response;
    }

    @Async
    @Scheduled(fixedDelay = 1000 * 60)
    public void updateRocChangeSignDFF() {
        List<DffCalculation> dffCalculationList = dffCalculationRepository.findAllByRocIsNotNullAndRocChangeSignIsNull();
        DffCalculation lastUpdatedRecord = dffCalculationRepository.findTopByRocIsNotNullAndRocChangeSignIsNotNullOrderByToDateDesc();

        Collections.sort(dffCalculationList, new FederalReserveService.SortByDateDffCalculation());
        if(dffCalculationList.size() == 0){
            return;
        }

        Float lastRoc = lastUpdatedRecord.getRoc();
        for (DffCalculation dffCalculation : dffCalculationList) {
            if(dffCalculation.getRoc() < lastRoc){
                dffCalculation.setRocChangeSign(-1);
            }else if (dffCalculation.getRoc() > lastRoc){
                dffCalculation.setRocChangeSign(1);
            }else if(dffCalculation.getRoc() == lastRoc){
                dffCalculation.setRocChangeSign(0);
            }

            lastRoc = dffCalculation.getRoc();
        }

        dffCalculationRepository.saveAll(dffCalculationList);
    }

}
