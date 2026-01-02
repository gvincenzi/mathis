package com.gist.mathis.service.finance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.model.repository.finance.TransactionRepository;

@Service
public class ExcelExportService {
	@Autowired
	private TransactionRepository transactionRepository;

	public byte[] generateExcelReport(int year) throws IOException {
		List<Transaction> allTransactions = transactionRepository.findByYearOrderByDateAsc(year);
		
		List<Transaction> allPreviousYearTransactions = transactionRepository.findByYearOrderByDateAsc(year-1);
		BigDecimal initialCashBalance = allPreviousYearTransactions.stream()
			    .filter(t -> TransactionType.CASH.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .reduce(BigDecimal.ZERO, BigDecimal::add);

			BigDecimal initialBankBalance = allPreviousYearTransactions.stream()
			    .filter(t -> TransactionType.BANK.equals(t.getType()))
			    .map(Transaction::getAmount)
			    .reduce(BigDecimal.ZERO, BigDecimal::add);
		
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			createCombinedSheet(workbook, allTransactions, initialCashBalance, initialBankBalance);
			workbook.write(out);
			return out.toByteArray();
		}
	}

	private void createCombinedSheet(XSSFWorkbook workbook, List<Transaction> transactions, BigDecimal initialCashBalance, BigDecimal initialBankBalance) {

		Sheet sheet = workbook.createSheet("Rapport");

		// Définition des largeurs de colonnes
		sheet.setColumnWidth(0, 15 * 256); // Colonne A: Data Reg.
		sheet.setColumnWidth(1, 60 * 256); // Colonne B: Denominazione e DESCRIZIONE
		sheet.setColumnWidth(2, 20 * 256); // Colonne C: n° e data doc.
		sheet.setColumnWidth(3, 20 * 256); // Colonne D: Voce
		sheet.setColumnWidth(4, 15 * 256); // Colonne E: ENTRATE (A) (CASSA)
		sheet.setColumnWidth(5, 15 * 256); // Colonne F: USCITE (B) (CASSA)
		sheet.setColumnWidth(6, 15 * 256); // Colonne G: ENTRATE (A) (BANCA)
		sheet.setColumnWidth(7, 15 * 256); // Colonne H: USCITE (B) (BANCA)

		int currentRow = 0; // Compteur de ligne pour la feuille

		// Ligne 1: En-têtes de colonnes principales
		Row headerRow = sheet.createRow(currentRow++);
		Cell headerCell0 = headerRow.createCell(0); // Colonne A
		headerCell0.setCellValue("Data Reg.");

		Cell headerCell1 = headerRow.createCell(1); // Colonne B
		headerCell1.setCellValue("Descrizione");

		Cell headerCell2 = headerRow.createCell(2); // Colonne C
		headerCell2.setCellValue("n° e data doc.");
		
		Cell headerCell3 = headerRow.createCell(3); // Colonne D
		headerCell3.setCellValue("Voce");

		Cell headerCell4 = headerRow.createCell(4); // Colonne E
		headerCell4.setCellValue("CASSA> ENTRATE (A)");

		Cell headerCell5 = headerRow.createCell(5); // Colonne F
		headerCell5.setCellValue("CASSA> USCITE (B)");

		Cell headerCell6 = headerRow.createCell(6); // Colonne G
		headerCell6.setCellValue("BANCA> ENTRATE (A)");

		Cell headerCell7 = headerRow.createCell(7); // Colonne H
		headerCell7.setCellValue("BANCA> USCITE (B)");

		// Ligne 8: Soldes initiaux sous les en-têtes (exemples)
		Row initialBalanceRow = sheet.createRow(currentRow++);
		Cell descriptionInitialBalanceCell = initialBalanceRow.createCell(1);
		descriptionInitialBalanceCell.setCellValue("Residui anno precedente");
		Cell initialCashBalanceCell = initialBalanceRow.createCell(initialCashBalance.compareTo(BigDecimal.ZERO) > 0 ?  4 : 5); // Colonne C (pour CASSA)
		initialCashBalanceCell.setCellValue(initialCashBalance.doubleValue());

		Cell initialBankBalanceCell = initialBalanceRow.createCell(initialBankBalance.compareTo(BigDecimal.ZERO) > 0 ? 7 : 8); // Colonne F (pour BANCA)
		initialBankBalanceCell.setCellValue(initialBankBalance.doubleValue());

		// --- Données des transactions ---
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ITALIAN);
		BigDecimal totalCashEntrate = BigDecimal.ZERO;
		BigDecimal totalCashUscite = BigDecimal.ZERO;
		BigDecimal totalBankEntrate = BigDecimal.ZERO;
		BigDecimal totalBankUscite = BigDecimal.ZERO;

		for (Transaction transaction : transactions) {
			Row dataRow = sheet.createRow(currentRow++);

			// Colonne A: Data Reg.
			Cell dateCell = dataRow.createCell(0);
			dateCell.setCellValue(transaction.getDate().format(dateFormatter));

			// Colonne B: Denominazione e DESCRIZIONE
			Cell descriptionCell = dataRow.createCell(1);
			descriptionCell.setCellValue(transaction.getDescription());
			
			Cell voceCell = dataRow.createCell(3); // Colonne D
			voceCell.setCellValue(transaction.getTransactionDetail() != null ? transaction.getTransactionDetail().getCode() : "");

			if (transaction.getType() == TransactionType.CASH) {
				if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					Cell docCell = dataRow.createCell(2); // Colonne C
					docCell.setCellValue(transaction.getFormattedDocumentNumber());
					
					Cell entrateCell = dataRow.createCell(4); // Colonne E
					entrateCell.setCellValue(transaction.getAmount().doubleValue());
					totalCashEntrate = totalCashEntrate.add(transaction.getAmount());
				} else {
					Cell usciteCell = dataRow.createCell(5); // Colonne F
					usciteCell.setCellValue(transaction.getAmount().abs().doubleValue());
					totalCashUscite = totalCashUscite.add(transaction.getAmount().abs());
				}

			} else if (transaction.getType() == TransactionType.BANK) {
				if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					Cell docCell = dataRow.createCell(2); // Colonne C
					docCell.setCellValue(transaction.getFormattedDocumentNumber());
					
					Cell entrateCell = dataRow.createCell(6); // Colonne G
					entrateCell.setCellValue(transaction.getAmount().doubleValue());
					totalBankEntrate = totalBankEntrate.add(transaction.getAmount());
				} else {
					Cell usciteCell = dataRow.createCell(7); // Colonne H
					usciteCell.setCellValue(transaction.getAmount().abs().doubleValue());
					totalBankUscite = totalBankUscite.add(transaction.getAmount().abs());
				}
			}
		}

		// --- Totaux du pied de page ---
		currentRow++; // Ligne vide avant les totaux

		// Ligne des valeurs totales (TOT. DARE / TOT. AVERE)
		Row totalRowValues = sheet.createRow(currentRow++);
		Cell totalCashDareValueCell = totalRowValues.createCell(4); // Colonne E
		totalCashDareValueCell.setCellValue(totalCashEntrate.doubleValue());
		Cell totalCashUsciteValueCell = totalRowValues.createCell(5); // Colonne F
		totalCashUsciteValueCell.setCellValue(totalCashUscite.doubleValue());

		Cell totalBankDareValueCell = totalRowValues.createCell(6); // Colonne G
		totalBankDareValueCell.setCellValue(totalBankEntrate.doubleValue());
		Cell totalBankUsciteValueCell = totalRowValues.createCell(7); // Colonne H
		totalBankUsciteValueCell.setCellValue(totalBankUscite.doubleValue());

		// Ligne des soldes finaux (SALDO A-B)
		Row saldoRow = sheet.createRow(currentRow++);
		Cell saldoCashLabelCell = saldoRow.createCell(3); // Colonne D
		saldoCashLabelCell.setCellValue("SALDO A-B");
		Cell saldoCashValueCell = saldoRow.createCell(4); // Colonne E
		saldoCashValueCell.setCellValue(totalCashEntrate.subtract(totalCashUscite).doubleValue() + initialCashBalance.doubleValue());

		Cell saldoBankLabelCell = saldoRow.createCell(5); // Colonne F
		saldoBankLabelCell.setCellValue("SALDO A-B");
		Cell saldoBankValueCell = saldoRow.createCell(6); // Colonne G
		saldoBankValueCell.setCellValue(totalBankEntrate.subtract(totalBankUscite).doubleValue() + initialBankBalance.doubleValue());
	}
}
