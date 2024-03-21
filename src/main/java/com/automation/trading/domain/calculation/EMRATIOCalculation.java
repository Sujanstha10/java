package com.automation.trading.domain.calculation;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
public class EMRATIOCalculation {

	private Float roc;
	private Integer rocChangeSign;
	private Boolean rocAnnRollAvgFlag = false;
	private Float rocAnnualRollingAvg;
	private Float rollingThreeMonAvg;
	@Id
	private Date toDate;
}
