package com.gist.mathis.controller.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.gist.mathis.controller.entity.TransactionForm;
import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.service.finance.CashFlowService;
import com.gist.mathis.service.finance.CashFlowService.CashFlowDTO;
import com.gist.mathis.service.finance.TransactionService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class CashFlowController {
	@Autowired
	private CashFlowService cashFlowService;
	
	@Autowired
	private TransactionService transactionService;
	
	@GetMapping("/web/cashflow")
    public String cashflow(@RequestParam("year") Integer year, Model model) {
		CashFlowDTO cashFlowReport = cashFlowService.prepareCashFlowReport(year);
		model.addAttribute("year",year);
		prepareCashFlow(model, cashFlowReport);

        return "cashflow";
    }
	
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/web/cashflow/edit")
    public String cashflowEdit(@RequestParam("year") Integer year, Model model) {
		CashFlowDTO cashFlowReport = cashFlowService.prepareCashFlowReport(year);
		model.addAttribute("year", year);
		prepareCashFlow(model, cashFlowReport);

        return "cashflow_edit";
    }

	private void prepareCashFlow(Model model, CashFlowDTO cashFlowReport) {
		model.addAttribute("transactions", cashFlowReport.getAllTransactions());
		model.addAttribute("transactionDetails", cashFlowReport.getAllTransactionDetails());
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
			    .reduce(BigDecimal.ZERO, BigDecimal::subtract)
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
			    .reduce(BigDecimal.ZERO, BigDecimal::subtract)
				);
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/web/cashflow/update/{year}")
	public String updateTransactions(@ModelAttribute("transactions") TransactionForm transactionForm, @PathVariable("year") Integer year) {
		List<Transaction> transactionsMapped = transactionForm.getTransactions().stream()
			    .map(row -> {
			        Transaction t = new Transaction();
			        t.setId(row.getId());
			        t.setDate(row.getDate());
			        t.setDescription(row.getDescription());
			        t.setDocumentItemNumber(row.getDocumentItemNumber());

			        if (row.getTransactionDetailId() != null) {
			            t.setTransactionDetail(cashFlowService.allTransactionDetails().get(row.getTransactionDetailId()));
			        } else {
			            t.setTransactionDetail(null);
			        }

			        // Ricostruisci amount e type
			        if (row.getCassaEntrate() != null && row.getCassaEntrate().compareTo(BigDecimal.ZERO) > 0) {
			            t.setType(TransactionType.CASH);
			            t.setAmount(row.getCassaEntrate());
			        } else if (row.getCassaUscite() != null && row.getCassaUscite().compareTo(BigDecimal.ZERO) > 0) {
			            t.setType(TransactionType.CASH);
			            t.setAmount(row.getCassaUscite().negate());
			        } else if (row.getBancaEntrate() != null && row.getBancaEntrate().compareTo(BigDecimal.ZERO) > 0) {
			            t.setType(TransactionType.BANK);
			            t.setAmount(row.getBancaEntrate());
			        } else if (row.getBancaUscite() != null && row.getBancaUscite().compareTo(BigDecimal.ZERO) > 0) {
			            t.setType(TransactionType.BANK);
			            t.setAmount(row.getBancaUscite().negate());
			        } else {
			            t.setType(null);
			            t.setAmount(BigDecimal.ZERO);
			        }

			        return t;
			    })
			    .collect(Collectors.toList());

		transactionService.saveAll(transactionsMapped);
	    return String.format("redirect:/web/cashflow?year=%d",year);
	}
}
