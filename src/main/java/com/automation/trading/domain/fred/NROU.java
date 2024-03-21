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
public class NROU implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -828726777875204500L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public NROU(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
