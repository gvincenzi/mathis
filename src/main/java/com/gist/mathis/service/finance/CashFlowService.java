package com.gist.mathis.service.finance;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionDetail;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.model.repository.finance.TransactionDetailRepository;
import com.gist.mathis.model.repository.finance.TransactionRepository;

import lombok.AllArgsConstructor;
import lombok.Data;

@Service
public class CashFlowService {
	@Autowired
	protected TransactionRepository transactionRepository;
	
	@Autowired
	protected TransactionDetailRepository transactionDetailRepository;

	public CashFlowDTO prepareCashFlowReport(Integer year) {
		List<Transaction> allTransactions = transactionRepository.findByYearOrderByDateAsc(year);
		List<TransactionDetail> allTransactionDetails = transactionDetailRepository.findAll();

		List<Transaction> allPreviousYearTransactions = transactionRepository.findResidualPreviousYear(year);
		BigDecimal initialCashBalance = allPreviousYearTransactions.stream()
				.filter(t -> TransactionType.CASH.equals(t.getType())).map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal initialBankBalance = allPreviousYearTransactions.stream()
				.filter(t -> TransactionType.BANK.equals(t.getType())).map(Transaction::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		
		return new CashFlowDTO(allTransactions, allTransactionDetails, initialCashBalance, initialBankBalance);
	}
	
	public Map<Long,TransactionDetail> allTransactionDetails() {
		return transactionDetailRepository.findAll().stream()
			    .collect(Collectors.toMap(TransactionDetail::getId, Function.identity()));
	}
	
	@Data
	@AllArgsConstructor
	public class CashFlowDTO{
		List<Transaction> allTransactions;
		List<TransactionDetail> allTransactionDetails;
		BigDecimal initialCashBalance;
		BigDecimal initialBankBalance;
	}
}
