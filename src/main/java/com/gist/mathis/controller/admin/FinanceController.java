package com.gist.mathis.controller.admin;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gist.mathis.controller.entity.TransactionRequest;
import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.service.finance.ExcelExportService;
import com.gist.mathis.service.finance.TransactionService;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

@Slf4j
@RestController
@RequestMapping("/api/admin/finance")
public class FinanceController {
	@Autowired
	private ExcelExportService excelExportService;
	
	@Autowired
	private TransactionService transactionService;
	
	@GetMapping("/export/{year}")
    public ResponseEntity<Resource> exportExcelFile(@PathVariable("year") Integer year) {
    	ByteArrayResource resource;
		try {
			resource = new ByteArrayResource(excelExportService.generateExcelReport(year));
	        String fileName = "finance_" + year + ".xlsx";
	        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").contentLength(resource.contentLength()).contentType(MediaType.APPLICATION_JSON).body(resource);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@PostMapping("/transaction")
	public ResponseEntity<Transaction> transaction(@RequestBody TransactionRequest transaction) {
		try {
			return new ResponseEntity<Transaction>(transactionService.addTransaction(transaction), HttpStatus.ACCEPTED);
		} catch (JsonProcessingException e) {
			return new ResponseEntity<Transaction>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@GetMapping("/transaction/receipt/{transactionId}")
    public ResponseEntity<Resource> getMemberCard(@PathVariable("transactionId") Long transactionId) {
    	ByteArrayResource resource;
		try {
			resource = new ByteArrayResource(transactionService.getTransactionReceipt(transactionId));
	        String fileName = "receipt#transaction#" + transactionId + ".pdf";
	        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").contentLength(resource.contentLength()).contentType(MediaType.APPLICATION_JSON).body(resource);
		} catch (NoSuchElementException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		} catch (JRException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
