package com.gist.mathis.controller.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.gist.mathis.model.entity.finance.TransactionType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Data
public class TransactionRequest {
	private LocalDate date;
	private String description;
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private TransactionType type;
}
