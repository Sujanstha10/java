package com.automation.trading.domain.calculation;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class T10YIECalculation {

	
	@Id
	private Date toDate;	
	private Float roc;
	private Integer rocChangeSign;
	private Boolean rocAnnRollAvgFlag = false;
	private Float rocAnnualRollingAvg;
	private Float rollingThreeMonAvg;
	
	

}
