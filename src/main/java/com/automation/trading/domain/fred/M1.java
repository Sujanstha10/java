package com.automation.trading.domain.fred;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 
 * @author niraj
 * 
 *         Entity class for M1 Money Stock
 *
 */
@Entity
@Data
@NoArgsConstructor
public class M1 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3753346894490523952L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public M1(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
