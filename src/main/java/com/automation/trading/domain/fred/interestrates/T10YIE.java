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
public class T10YIE implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6631885204604955868L;

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

	public T10YIE() {

	}

	public T10YIE(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}

}
