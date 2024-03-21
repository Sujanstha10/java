package com.automation.trading.repository;

import java.util.Date;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automation.trading.domain.fred.interestrates.T5YIFR;

public interface T5YIFRRepository extends JpaRepository<T5YIFR, Date>{

}
