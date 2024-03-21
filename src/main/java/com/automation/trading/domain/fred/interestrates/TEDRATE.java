package com.automation.trading.domain.fred.interestrates;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class TEDRATE implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5134300007901751075L;

	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO) private Long id;
	 */

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public TEDRATE(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}

}
