package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class IC4WSA implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7166909948907799453L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public IC4WSA(Date date, Float value) {
		this.date = date;
		this.value = value;
	}
}
