package com.automation.trading.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import com.automation.trading.domain.fred.PAYEMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.automation.trading.domain.calculation.PAYEMSCalculation;
import com.automation.trading.repository.PAYEMSCalculationRepository;
import com.automation.trading.repository.PAYEMSRepository;

@Service
public class PAYEMSService {

    @Autowired
    private PAYEMSRepository payemsRepository;

    @Autowired
    private PAYEMSCalculationRepository payemsCalculationRepository;

    public void calculateRoc() {

        List<PAYEMS> payemsList = payemsRepository.findAll();
        List<PAYEMSCalculation> payemsCalculationModified = new ArrayList<>();
        Queue<PAYEMS> payemsQueue = new LinkedList<>();
        for (PAYEMS payems : payemsList) {
            if (payems.getRocFlag()) {
                continue;
            }
            Float roc = 0.0f;
            int period = 0;
            PAYEMSCalculation payemsCalculation = new PAYEMSCalculation();
            if (payemsQueue.size() == 2) {
                payemsQueue.poll();
            }
            payemsQueue.add(payems);
            Iterator<PAYEMS> queueIterator = payemsQueue.iterator();
            while (queueIterator.hasNext()) {
                PAYEMS temp = queueIterator.next();
                temp.setRocFlag(true);
            }


            if (payemsQueue.size() == 1) {
                roc = 0f;
                payemsCalculation.setRoc(roc);
                payemsCalculation.setToDate(payems.getDate());
            } else {
                roc = (payems.getValue() / ((LinkedList<PAYEMS>) payemsQueue).get(0).getValue()) - 1;
                payemsCalculation.setRoc(roc);
                payemsCalculation.setToDate(payems.getDate());
            }
            payemsCalculationModified.add(payemsCalculation);
        }

        payemsList = payemsRepository.saveAll(payemsList);
        payemsCalculationModified =payemsCalculationRepository.saveAll(payemsCalculationModified);

    }

    public List<PAYEMSCalculation> calculateRollAvgThreeMonth() {
        List<PAYEMSCalculation> payemsCalculationList = new ArrayList<>();
        List<PAYEMS> payemsList = payemsRepository.findAll();
        List<PAYEMSCalculation> payemsCalculationReference = payemsCalculationRepository.findAll();
        Queue<PAYEMS> payemsQueue = new LinkedList<>();

        for (PAYEMS payems : payemsList) {

            if (payemsQueue.size() == 3) {
                payemsQueue.poll();
            }
            payemsQueue.add(payems);

            if (payems.getRollAverageFlag()) {
                continue;
            }
            Float rollingAvg = 0.0f;
            Float rollingAvgThreeMon = 0f;
            int period = 0;

            Iterator<PAYEMS> queueItr = payemsQueue.iterator();

            PAYEMSCalculation tempGdpCalculation = new PAYEMSCalculation();
            List<PAYEMSCalculation> currentPAYEMSCalculationRef = payemsCalculationReference.stream()
                    .filter(p -> p.getToDate().equals(payems.getDate())).collect(Collectors.toList());

            if (currentPAYEMSCalculationRef.size() > 0)
                tempGdpCalculation = currentPAYEMSCalculationRef.get(0);

            while (queueItr.hasNext()) {
                PAYEMS gdpVal = queueItr.next();
                rollingAvg += gdpVal.getValue();
                period++;
            }

            rollingAvgThreeMon = rollingAvg / period;

            payems.setRollAverageFlag(true);
            tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
            payemsCalculationList.add(tempGdpCalculation);

        }

        payemsCalculationReference = payemsCalculationRepository.saveAll(payemsCalculationList);
        payemsList = payemsRepository.saveAll(payemsList);
        return payemsCalculationReference;
    }

    public List<PAYEMSCalculation> calculateRocRollingAnnualAvg() {

        List<PAYEMSCalculation> payemsCalculationReference = payemsCalculationRepository.findAll();
        Queue<PAYEMSCalculation> payemsCalculationPriorityQueue = new LinkedList<>();
        for (PAYEMSCalculation payemsCalculation : payemsCalculationReference) {
            Float rocFourMonth = 0.0f;
            Float rocFourMonthAvg = 0.0f;
            int period = 0;
            if (payemsCalculationPriorityQueue.size() == 4) {
                payemsCalculationPriorityQueue.poll();
            }
            payemsCalculationPriorityQueue.add(payemsCalculation);

            if (payemsCalculation.getRocAnnRollAvgFlag()) {
                continue;
            }
            Iterator<PAYEMSCalculation> queueIterator = payemsCalculationPriorityQueue.iterator();
            while (queueIterator.hasNext()) {
                PAYEMSCalculation temp = queueIterator.next();
                rocFourMonth += temp.getRoc();
                period++;
            }
            rocFourMonthAvg = rocFourMonth / period;
            payemsCalculation.setRocAnnRollAvgFlag(true);
            payemsCalculation.setRocAnnualRollingAvg(rocFourMonthAvg);
        }
        System.out.println(payemsCalculationReference);
        payemsCalculationReference = payemsCalculationRepository.saveAll(payemsCalculationReference);
        return payemsCalculationReference;
    }

    public List<PAYEMSCalculation> updateRocChangeSignPAYEMS() {
        List<PAYEMSCalculation> payemsCalculationList = payemsCalculationRepository.findAllByRocIsNotNull();

        if(payemsCalculationList.isEmpty()){
            return payemsCalculationList;
        }
        List<PAYEMSCalculation> modifiedSignList = new ArrayList<>();
        PAYEMSCalculation payemsCalculationPrev = new PAYEMSCalculation();

        for (PAYEMSCalculation payemsCalculation : payemsCalculationList) {
            PAYEMSCalculation modifiedSigndffCalc = payemsCalculation;
            if (payemsCalculationPrev.getToDate() == null) {
                modifiedSigndffCalc.setRocChangeSign(0);
            } else {
                if (payemsCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(1);
                } else if (payemsCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
                    modifiedSigndffCalc.setRocChangeSign(-1);
                } else {
                    modifiedSigndffCalc.setRocChangeSign(0);
                }
            }
            modifiedSignList.add(modifiedSigndffCalc);
            payemsCalculationPrev = modifiedSigndffCalc;
        }
        payemsCalculationList = payemsCalculationRepository.saveAll(modifiedSignList);
        return payemsCalculationList;
    }

}
