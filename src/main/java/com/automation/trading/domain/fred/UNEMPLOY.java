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
public class UNEMPLOY implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9119486149217318601L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public UNEMPLOY(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

}
