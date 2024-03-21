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
public class DSPI implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2890191138580008364L;

	@Id
	private Date date;
	private Float value;

	public DSPI(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}

}
