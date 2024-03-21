package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import com.automation.trading.domain.fred.MANEMP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.MANEMPCalculation;
import com.automation.trading.repository.MANEMPCalculationRepository;
import com.automation.trading.repository.MANEMPRepository;

@Service
public class MANEMPService {

    @Autowired
    private MANEMPRepository manempRepository;

    @Autowired
    private MANEMPCalculationRepository manempCalculationRepository;

    public void calculateRoc() {

        List<MANEMP> manempList = manempRepository.findAll();
        List<MANEMPCalculation> manempCalculationModified = new ArrayList<>();
        Queue<MANEMP> manempQueue = new LinkedList<>();
        for (MANEMP manemp : manempList) {
            if (manemp.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;
            int period = 0;
            MANEMPCalculation manempCalculation = new MANEMPCalculation();
            if (manempQueue.size() == 2) {
                manempQueue.poll();
            }
            manempQueue.add(manemp);
            Iterator<MANEMP> queueIterator = manempQueue.iterator();
            while (queueIterator.hasNext()) {
                MANEMP temp = queueIterator.next();
                temp.setRocFlag(true);
            }


            if (manempQueue.size() == 1) {
                roc = 0f;
                manempCalculation.setRoc(roc);
                manempCalculation.setToDate(manemp.getDate());
            } else {
                roc = (manemp.getValue() / ((LinkedList<MANEMP>) manempQueue).get(0).getValue()) - 1;
                manempCalculation.setRoc(roc);
                manempCalculation.setToDate(manemp.getDate());
            }
            manempCalculationModified.add(manempCalculation);
        }

        manempList = manempRepository.saveAll(manempList);
        manempCalculationModified =manempCalculationRepository.saveAll(manempCalculationModified);

    }

    public List<MANEMPCalculation> calculateRollAvgThreeMonth() {
        List<MANEMPCalculation> manempCalculationList = new ArrayList<>();
        List<MANEMP> manempList = manempRepository.findAll();
        List<MANEMPCalculation> manempCalculationReference = manempCalculationRepository.findAll();
        Queue<MANEMP> manempQueue = new LinkedList<>();

        for (MANEMP manemp : manempList) {

            if (manempQueue.size() == 3) {
                manempQueue.poll();
            }
            manempQueue.add(manemp);

            if (manemp.getRollAverageFlag()) {
                continue;
            }
            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            Iterator<MANEMP> queueItr = manempQueue.iterator();

            MANEMPCalculation tempGdpCalculation = new MANEMPCalculation();
            List<MANEMPCalculation> currentMANEMPCalculationRef = manempCalculationReference.stream()
                    .filter(p -> p.getToDate().equals(manemp.getDate())).collect(Collectors.toList());

            if (currentMANEMPCalculationRef.size() > 0)
                tempGdpCalculation = currentMANEMPCalculationRef.get(0);

            while (queueItr.hasNext()) {
                MANEMP gdpVal = queueItr.next();
                rollingAvg += gdpVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            manemp.setRollAverageFlag(true);
            tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            manempCalculationList.add(tempGdpCalculation);

        }

        manempCalculationReference = manempCalculationRepository.saveAll(manempCalculationList);
        manempList = manempRepository.saveAll(manempList);
        return manempCalculationReference;
    }

    public List<MANEMPCalculation> calculateRocRollingAnnualAvg() {

        List<MANEMPCalculation> manempCalculationReference = manempCalculationRepository.findAll();
        Queue<MANEMPCalculation> manempCalculationPriorityQueue = new LinkedList<>();
        for (MANEMPCalculation manempCalculation : manempCalculationReference) {
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
        System.out.println(manempCalculationReference);
        manempCalculationReference = manempCalculationRepository.saveAll(manempCalculationReference);
        return manempCalculationReference;
    }

    public List<MANEMPCalculation> updateRocChangeSignMANEMP() {
        List<MANEMPCalculation> manempCalculationList = manempCalculationRepository.findAllByRocIsNotNull();

        if(manempCalculationList.isEmpty()){
            return manempCalculationList;
        }
        List<MANEMPCalculation> modifiedSignList = new ArrayList<>();
        MANEMPCalculation manempCalculationPrev = new MANEMPCalculation();

        for (MANEMPCalculation manempCalculation : manempCalculationList) {
            MANEMPCalculation modifiedSigndffCalc = manempCalculation;
            if (manempCalculationPrev.getToDate() == null) {
                modifiedSigndffCalc.setRocChangeSign(0);
            } else {
                if (manempCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(1);
                } else if (manempCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(-1);
                } else {
                    modifiedSigndffCalc.setRocChangeSign(0);
                }
            }
            modifiedSignList.add(modifiedSigndffCalc);
            manempCalculationPrev = modifiedSigndffCalc;
        }
        manempCalculationList = manempCalculationRepository.saveAll(modifiedSignList);
        return manempCalculationList;
    }

}
