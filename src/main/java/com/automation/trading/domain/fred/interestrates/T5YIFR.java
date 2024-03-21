package com.automation.trading.domain.fred.interestrates;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class T5YIFR implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6974819561308115058L;
	
	/*
	 * @Id
	 * 
	 * @GeneratedValue(strategy = GenerationType.AUTO) private Long id;
	 */

	@Id
	private Date date;

	private Float value;

	public T5YIFR(Date date, float parseFloat) {
		this.date = date;
		this.value = parseFloat;
	}

}
