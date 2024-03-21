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
public class T5YIE implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7459010224550356096L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public T5YIE(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

	public T5YIE() {

	}

}
