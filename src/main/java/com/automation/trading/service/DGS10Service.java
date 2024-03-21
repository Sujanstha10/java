package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.DGS10Calculation;
import com.automation.trading.domain.fred.interestrates.DGS10;
import com.automation.trading.repository.DGS10CalculationRepository;
import com.automation.trading.repository.DGS10Repository;

@Service
public class DGS10Service {

    @Autowired
    private DGS10Repository dgs10Repository;

    @Autowired
    private DGS10CalculationRepository dgs10CalculationRepository;

    public void calculateRoc() {

        List<DGS10> dgs10List = dgs10Repository.findAll();
        //List<DGS10Calculation> dgs10CalculationList = dgs10CalculationRepository.findAll();
        List<DGS10Calculation> dgs10CalculationModified = new ArrayList<>();
        Queue<DGS10> dgs10Queue = new LinkedList<>();
        for (DGS10 dgs10 : dgs10List) {
            if (dgs10.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;
            int period = 0;
            DGS10Calculation dgs10Calculation = new DGS10Calculation();
            if (dgs10Queue.size() == 2) {
                dgs10Queue.poll();
            }
            dgs10Queue.add(dgs10);
            Iterator<DGS10> queueIterator = dgs10Queue.iterator();
            while (queueIterator.hasNext()) {
                DGS10 temp = queueIterator.next();
                temp.setRocFlag(true);
            }


            if (dgs10Queue.size() == 1) {
                roc = 0f;
                dgs10Calculation.setRoc(roc);
                dgs10Calculation.setToDate(dgs10.getDate());
            } else {
                roc = (dgs10.getValue() / ((LinkedList<DGS10>) dgs10Queue).get(0).getValue()) - 1;
                dgs10Calculation.setRoc(roc);
                dgs10Calculation.setToDate(dgs10.getDate());
            }
            dgs10CalculationModified.add(dgs10Calculation);
        }

        dgs10List = dgs10Repository.saveAll(dgs10List);
        dgs10CalculationModified =dgs10CalculationRepository.saveAll(dgs10CalculationModified);

    }

    public List<DGS10Calculation> calculateRollAvgThreeMonth() {
        List<DGS10Calculation> dgs10CalculationList = new ArrayList<>();
        List<DGS10> dgs10List = dgs10Repository.findAll();
        List<DGS10Calculation> dgs10CalculationReference = dgs10CalculationRepository.findAll();
        Queue<DGS10> dgs10Queue = new LinkedList<>();

        for (DGS10 dgs10 : dgs10List) {

            if (dgs10Queue.size() == 3) {
                dgs10Queue.poll();
            }
            dgs10Queue.add(dgs10);

            if (dgs10.getRollAverageFlag()) {
                continue;
            }
            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            Iterator<DGS10> queueItr = dgs10Queue.iterator();

            DGS10Calculation tempGdpCalculation = new DGS10Calculation();
            List<DGS10Calculation> currentDGS10CalculationRef = dgs10CalculationReference.stream()
                    .filter(p -> p.getToDate().equals(dgs10.getDate())).collect(Collectors.toList());

            if (currentDGS10CalculationRef.size() > 0)
                tempGdpCalculation = currentDGS10CalculationRef.get(0);

            while (queueItr.hasNext()) {
                DGS10 gdpVal = queueItr.next();
                rollingAvg += gdpVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            dgs10.setRollAverageFlag(true);
            tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            dgs10CalculationList.add(tempGdpCalculation);

        }

        dgs10CalculationReference = dgs10CalculationRepository.saveAll(dgs10CalculationList);
        dgs10List = dgs10Repository.saveAll(dgs10List);
        return dgs10CalculationReference;
    }

    public List<DGS10Calculation> calculateRocRollingAnnualAvg() {

        List<DGS10Calculation> dgs10CalculationReference = dgs10CalculationRepository.findAll();
        Queue<DGS10Calculation> dgs10CalculationPriorityQueue = new LinkedList<>();
        for (DGS10Calculation dgs10Calculation : dgs10CalculationReference) {
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
        System.out.println(dgs10CalculationReference);
        dgs10CalculationReference = dgs10CalculationRepository.saveAll(dgs10CalculationReference);
        return dgs10CalculationReference;
    }

    public List<DGS10Calculation> updateRocChangeSignDgs10() {
        List<DGS10Calculation> dgs10CalculationList = dgs10CalculationRepository.findAllByRocIsNotNull();

        if(dgs10CalculationList.isEmpty()){
            return dgs10CalculationList;
        }
        List<DGS10Calculation> modifiedSignList = new ArrayList<>();
        DGS10Calculation dgs10CalculationPrev = new DGS10Calculation();

        for (DGS10Calculation dgs10Calculation : dgs10CalculationList) {
            DGS10Calculation modifiedSigndffCalc = dgs10Calculation;
            if (dgs10CalculationPrev.getToDate() == null) {
                modifiedSigndffCalc.setRocChangeSign(0);
            } else {
                if (dgs10CalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(1);
                } else if (dgs10CalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(-1);
                } else {
                    modifiedSigndffCalc.setRocChangeSign(0);
                }
            }
            modifiedSignList.add(modifiedSigndffCalc);
            dgs10CalculationPrev = modifiedSigndffCalc;
        }
        dgs10CalculationList = dgs10CalculationRepository.saveAll(modifiedSignList);
        return dgs10CalculationList;
    }

}
