package com.gist.mathis.model.repository.finance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gist.mathis.model.entity.finance.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
	@Query(value = "SELECT * FROM transactions WHERE EXTRACT(YEAR FROM date) = :year ORDER BY date ASC", nativeQuery = true)
	List<Transaction> findByYearOrderByDateAsc(@Param("year") int year);
	
	@Query(value = "SELECT MAX(document_item_number) FROM transactions WHERE EXTRACT(YEAR FROM date) = :year", nativeQuery = true)
	Integer findLastDocumentItemNumber(@Param("year") int year);
}