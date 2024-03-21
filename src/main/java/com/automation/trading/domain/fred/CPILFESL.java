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
 * Entity for Consumer Price Index for All Urban Consumers: All Items Less Food and Energy
 */

@Entity
@Data
@NoArgsConstructor
public class CPILFESL implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -2919231328327600066L;

	@Id
	private Date date;

	private Float value;

	public CPILFESL(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}


}
