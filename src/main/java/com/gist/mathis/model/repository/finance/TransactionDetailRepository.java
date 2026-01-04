package com.gist.mathis.model.repository.finance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.gist.mathis.model.entity.finance.TransactionDetail;

@Repository
public interface TransactionDetailRepository extends JpaRepository<TransactionDetail, Long> {
	List<TransactionDetail> findAll();
}