/*
 * package com.automation.trading.service;
 * 
 * import java.util.ArrayList; import java.util.Iterator; import
 * java.util.LinkedList; import java.util.List; import java.util.Queue; import
 * java.util.stream.Collectors;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.stereotype.Service;
 * 
 * import com.automation.trading.domain.calculation.T5YIFRCalculation; import
 * com.automation.trading.domain.fred.interestrates.T5YIFR; import
 * com.automation.trading.repository.T5YIFRCalculationRepository; import
 * com.automation.trading.repository.T5YIFRRepository;
 * 
 * @Service public class T5YIFRService {
 * 
 * @Autowired private T5YIFRRepository t5yifrRepository;
 * 
 * @Autowired private T5YIFRCalculationRepository t5yifrCalculationRepository;
 * 
 * public void calculateRoc() { List<T5YIFR> t5yifrList =
 * t5yifrRepository.findAll(); List<T5YIFRCalculation> t5yifrCalculationList =
 * t5yifrCalculationRepository.findAll(); List<T5YIFRCalculation>
 * t5yifrCalculationModified = new ArrayList<>(); Queue<T5YIFR> t5yifrQueue =
 * new LinkedList<>(); for (T5YIFR t5yifr : t5yifrList) { if
 * (t5yifr.getRocFlag()) { continue; } Float roc = 0.0f; int period = 0;
 * T5YIFRCalculation baseCalculation = new T5YIFRCalculation(); if
 * (t5yifrQueue.size() == 2) { t5yifrQueue.poll(); } t5yifrQueue.add(t5yifr);
 * Iterator<T5YIFR> queueIterator = t5yifrQueue.iterator(); while
 * (queueIterator.hasNext()) { T5YIFR temp = queueIterator.next();
 * temp.setRocFlag(true); }
 * 
 * List<T5YIFRCalculation> currentT5YIFRCalculationRef =
 * t5yifrCalculationList.stream() .filter(p ->
 * p.getToDate().equals(t5yifr.getDate())).collect(Collectors.toList());
 * 
 * if (currentT5YIFRCalculationRef.size() > 0) baseCalculation =
 * currentT5YIFRCalculationRef.get(0);
 * 
 * if (t5yifrQueue.size() == 1) { roc = 0f; baseCalculation.setRoc(roc);
 * baseCalculation.setToDate(t5yifr.getDate()); } else { roc =
 * (t5yifr.getValue() / ((LinkedList<T5YIFR>) t5yifrQueue).get(0).getValue()) -
 * 1; baseCalculation.setRoc(roc); baseCalculation.setToDate(t5yifr.getDate());
 * } t5yifrCalculationModified.add(baseCalculation); }
 * 
 * t5yifrRepository.saveAll(t5yifrList);
 * t5yifrCalculationRepository.saveAll(t5yifrCalculationModified);
 * 
 * }
 * 
 * public List<T5YIFRCalculation> calculateRollAvgThreeMonth() {
 * List<T5YIFRCalculation> t5yifrCalculationList = new ArrayList<>();
 * List<T5YIFR> t5yifrList = t5yifrRepository.findAll(); List<T5YIFRCalculation>
 * t5yifrCalculationReference = t5yifrCalculationRepository.findAll();
 * Queue<T5YIFR> t5yifrQueue = new LinkedList<>();
 * 
 * for (T5YIFR t5yifr : t5yifrList) {
 * 
 * if (t5yifrQueue.size() == 3) { t5yifrQueue.poll(); } t5yifrQueue.add(t5yifr);
 * 
 * if (t5yifr.getRollAverageFlag()) { continue; } Float rollingAvg = 0.0f; Float
 * rollingAvgThreeMon = 0f; int period = 0;
 * 
 * Iterator<T5YIFR> queueItr = t5yifrQueue.iterator();
 * 
 * T5YIFRCalculation tempGdpCalculation = new T5YIFRCalculation();
 * List<T5YIFRCalculation> currentT5YIFRCalculationRef =
 * t5yifrCalculationReference.stream() .filter(p ->
 * p.getToDate().equals(t5yifr.getDate())).collect(Collectors.toList());
 * 
 * if (currentT5YIFRCalculationRef.size() > 0) tempGdpCalculation =
 * currentT5YIFRCalculationRef.get(0);
 * 
 * while (queueItr.hasNext()) { T5YIFR gdpVal = queueItr.next(); rollingAvg +=
 * gdpVal.getValue(); period++; }
 * 
 * rollingAvgThreeMon = rollingAvg / period;
 * 
 * t5yifr.setRollAverageFlag(true);
 * tempGdpCalculation.setRollingThreeMonAvg(rollingAvgThreeMon);
 * t5yifrCalculationList.add(tempGdpCalculation);
 * 
 * }
 * 
 * t5yifrCalculationReference =
 * t5yifrCalculationRepository.saveAll(t5yifrCalculationList); t5yifrList =
 * t5yifrRepository.saveAll(t5yifrList); return t5yifrCalculationReference; }
 * 
 * public List<T5YIFRCalculation> calculateRocRollingAnnualAvg() {
 * 
 * List<T5YIFRCalculation> t5yifrCalculationReference =
 * t5yifrCalculationRepository.findAll(); Queue<T5YIFRCalculation>
 * t5yifrCalculationPriorityQueue = new LinkedList<>(); for (T5YIFRCalculation
 * t5yifrCalculation : t5yifrCalculationReference) { Float rocFourMonth = 0.0f;
 * Float rocFourMonthAvg = 0.0f; int period = 0; if
 * (t5yifrCalculationPriorityQueue.size() == 4) {
 * t5yifrCalculationPriorityQueue.poll(); }
 * t5yifrCalculationPriorityQueue.add(t5yifrCalculation);
 * 
 * if (t5yifrCalculation.getRocAnnRollAvgFlag()) { continue; }
 * Iterator<T5YIFRCalculation> queueIterator =
 * t5yifrCalculationPriorityQueue.iterator(); while (queueIterator.hasNext()) {
 * T5YIFRCalculation temp = queueIterator.next(); rocFourMonth += temp.getRoc();
 * period++; } rocFourMonthAvg = rocFourMonth / period;
 * t5yifrCalculation.setRocAnnRollAvgFlag(true);
 * t5yifrCalculation.setRocAnnualRollingAvg(rocFourMonthAvg); }
 * System.out.println(t5yifrCalculationReference); t5yifrCalculationReference =
 * t5yifrCalculationRepository.saveAll(t5yifrCalculationReference); return
 * t5yifrCalculationReference; }
 * 
 * public List<T5YIFRCalculation> updateRocChangeSignDff() {
 * List<T5YIFRCalculation> t5yifrCalculationList =
 * t5yifrCalculationRepository.findAll(); List<T5YIFRCalculation>
 * modifiedSignList = new ArrayList<>(); T5YIFRCalculation t5yifrCalculationPrev
 * = new T5YIFRCalculation();
 * 
 * for (T5YIFRCalculation t5yifrCalculation : t5yifrCalculationList) {
 * T5YIFRCalculation modifiedSigndffCalc = t5yifrCalculation; if
 * (t5yifrCalculationPrev.getId() == null) {
 * modifiedSigndffCalc.setRocChangeSign(0); } else { if
 * (t5yifrCalculationPrev.getRoc() < modifiedSigndffCalc.getRoc()) {
 * modifiedSigndffCalc.setRocChangeSign(1); } else if
 * (t5yifrCalculationPrev.getRoc() > modifiedSigndffCalc.getRoc()) {
 * modifiedSigndffCalc.setRocChangeSign(-1); } else {
 * modifiedSigndffCalc.setRocChangeSign(0); } }
 * modifiedSignList.add(modifiedSigndffCalc); t5yifrCalculationPrev =
 * modifiedSigndffCalc; } t5yifrCalculationList =
 * t5yifrCalculationRepository.saveAll(modifiedSignList); return
 * t5yifrCalculationList; } }
 */