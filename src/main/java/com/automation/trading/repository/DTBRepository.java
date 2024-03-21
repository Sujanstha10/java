package com.automation.trading.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.automation.trading.domain.fred.interestrates.DTB3;

public interface DTBRepository extends JpaRepository<DTB3, Long> {

}
