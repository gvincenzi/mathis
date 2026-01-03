package com.gist.mathis.service.finance;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.model.repository.finance.TransactionRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class CashFlowService {
	@Autowired
	protected TransactionRepository transactionRepository;

	public CashFlowDTO prepareCashFlowReport(Integer year) {
		List<Transaction> allTransactions = transactionRepository.findByYearOrderByDateAsc(year);

		List<Transaction> allPreviousYearTransactions = transactionRepository.findResidualPreviousYear(year);
		BigDecimal initialCashBalance = allPreviousYearTransactions.stream()
				.filter(t -> TransactionType.CASH.equals(t.getType())).map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal initialBankBalance = allPreviousYearTransactions.stream()
				.filter(t -> TransactionType.BANK.equals(t.getType())).map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		return new CashFlowDTO(allTransactions, initialCashBalance, initialBankBalance);
	}
	
	@Data
	@AllArgsConstructor
	public class CashFlowDTO{
		List<Transaction> allTransactions;
		BigDecimal initialCashBalance;
		BigDecimal initialBankBalance;
	}
}
