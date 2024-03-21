package com.automation.trading.domain.fred.interestrates;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class DTB3 implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8251424816095103323L;

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
    
    public DTB3(Date date, float parseFloat) {
		this.date=date;
		this.value=parseFloat;
	}

	public DTB3(){

    }

}
