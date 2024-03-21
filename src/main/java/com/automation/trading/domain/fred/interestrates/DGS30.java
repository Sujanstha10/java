package com.automation.trading.domain.fred.interestrates;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class DGS30 implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3913621362724996211L;

//	@Id
//    @GeneratedValue(strategy = GenerationType.SEQUENCE)
//    @Id
//    @GeneratedValue(
//            strategy = GenerationType.SEQUENCE,
//            generator = "post_sequence"
//    )
//    @SequenceGenerator(
//            name = "post_sequence",
//            sequenceName = "post_sequence",
//            allocationSize = 100
//    )
//    private Long id;
    @Id
    private Date date;
    private Float value;
    private Boolean rocFlag = false;
    private Boolean rollAverageFlag = false;

    public DGS30(Date date,Float value){
        this.date = date;
        this.value =value;
    }

    public DGS30(){

    }

}
