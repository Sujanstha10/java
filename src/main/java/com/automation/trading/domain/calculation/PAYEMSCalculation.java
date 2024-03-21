package com.automation.trading.domain.calculation;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Date;

@Entity
@Data
public class PAYEMSCalculation {
    @Id
    private Date toDate;
    private Float roc;
    private Integer rocChangeSign;
    private Boolean rocAnnRollAvgFlag = false;
    private Float rocAnnualRollingAvg;
    private Float rollingThreeMonAvg;
}
