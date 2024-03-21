package com.automation.trading.domain.fred;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
@Data
@Entity
public class UnRate {

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.SEQUENCE) private Long id;
	 */

	@Id
    private Date date;
    private Float value;
    private Boolean rocFlag = false;
    private Boolean rollAverageFlag = false;
    
    public UnRate(Date date, float parseFloat) {
		this.date=date;
		this.value=parseFloat;
	}

	public UnRate(){

    }


}
