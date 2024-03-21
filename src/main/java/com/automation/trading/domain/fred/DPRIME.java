package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
public class DPRIME implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4457844444890455530L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public DPRIME(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
