package com.gist.mathis.controller.web;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.service.finance.CashFlowService;
import com.gist.mathis.service.finance.CashFlowService.CashFlowDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CashFlowController {
	@Autowired
	private CashFlowService cashFlowService;
	
	@GetMapping("/web/cashflow")
    public String getBookForm(@RequestParam("year") Integer year, Model model) {
		CashFlowDTO cashFlowReport = cashFlowService.prepareCashFlowReport(year);
		
		model.addAttribute("transactions", cashFlowReport.getAllTransactions());
		model.addAttribute("initialCashBalance", cashFlowReport.getInitialCashBalance());
		model.addAttribute("initialBankBalance", cashFlowReport.getInitialBankBalance());
		model.addAttribute("totalCashEntrate", 
				cashFlowReport.getAllTransactions().stream()
			    .filter(t -> TransactionType.CASH.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
			    .reduce(BigDecimal.ZERO, BigDecimal::add)
				);
		model.addAttribute("totalCashUscite", 
				cashFlowReport.getAllTransactions().stream()
			    .filter(t -> TransactionType.CASH.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
			    .reduce(BigDecimal.ZERO, BigDecimal::add)
				);
		model.addAttribute("totalBankEntrate", 
				cashFlowReport.getAllTransactions().stream()
			    .filter(t -> TransactionType.BANK.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
			    .reduce(BigDecimal.ZERO, BigDecimal::add)
				);
		model.addAttribute("totalBankUscite", 
				cashFlowReport.getAllTransactions().stream()
			    .filter(t -> TransactionType.BANK.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
			    .reduce(BigDecimal.ZERO, BigDecimal::add)
				);

        return "cashflow";
    }
}
