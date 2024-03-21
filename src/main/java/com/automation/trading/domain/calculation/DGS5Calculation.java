package com.automation.trading.domain.calculation;

import java.util.Date;

import javax.persistence.*;

import lombok.Data;

@Entity
@Data
public class DGS5Calculation {

	private Float roc;
	private Integer rocChangeSign;
	private Boolean rocAnnRollAvgFlag = false;
	private Float rocAnnualRollingAvg;
	private Float rollingThreeMonAvg;
	@Id
	private Date toDate;

}
