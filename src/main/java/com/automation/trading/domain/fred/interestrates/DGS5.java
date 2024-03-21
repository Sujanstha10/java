package com.automation.trading.domain.fred.interestrates;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class DGS5 implements Serializable {

	private static final long serialVersionUID = 8251424816095103323L;

	@Id
	private Date date;
	private Float value;
	private Boolean rocFlag = false;
	private Boolean rollAverageFlag = false;

	public DGS5(Date date, Float value) {
		this.date = date;
		this.value = value;
	}

	public DGS5() {

	}

}
