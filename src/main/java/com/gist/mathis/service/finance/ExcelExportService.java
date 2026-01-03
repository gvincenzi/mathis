package com.gist.mathis.service.finance;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionType;
import com.gist.mathis.model.repository.finance.TransactionRepository;

@Service
public class ExcelExportService {
	XSSFColor gray = new XSSFColor(new byte[] { (byte) 211, (byte) 211, (byte) 211 }, null);
	XSSFColor red = new XSSFColor(new byte[] { (byte) 255, (byte) 0, (byte) 0 }, null);
	XSSFColor green = new XSSFColor(new byte[] { (byte) 0, (byte) 128, (byte) 0 }, null);
	XSSFColor orange = new XSSFColor(new byte[] { (byte) 255, (byte) 165, (byte) 0 }, null);
	XSSFColor black = new XSSFColor(new byte[] { (byte) 0, (byte) 0, (byte) 0 }, null);
	XSSFColor white = new XSSFColor(new byte[] { (byte) 255, (byte) 255, (byte) 255 }, null);
	
	@Autowired
	private TransactionRepository transactionRepository;

	public byte[] generateExcelReport(int year) throws IOException {
		List<Transaction> allTransactions = transactionRepository.findByYearOrderByDateAsc(year);
		
		List<Transaction> allPreviousYearTransactions = transactionRepository.findResidualPreviousYear(year);
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
		sheet.setColumnWidth(3, 10 * 256); // Colonne D: Voce
		sheet.setColumnWidth(4, 20 * 256); // Colonne E: ENTRATE (A) (CASSA)
		sheet.setColumnWidth(5, 20 * 256); // Colonne F: USCITE (B) (CASSA)
		sheet.setColumnWidth(6, 20 * 256); // Colonne G: ENTRATE (A) (BANCA)
		sheet.setColumnWidth(7, 20 * 256); // Colonne H: USCITE (B) (BANCA)

		int currentRow = 0; // Compteur de ligne pour la feuille

		// Ligne 1: En-têtes de colonnes principales
		Row headerRow = sheet.createRow(currentRow++);
		Cell headerCell0 = headerRow.createCell(0); // Colonne A
		headerCell0.setCellValue("Data Reg.");
		headerCell0.setCellStyle(createBoldStyle(workbook, gray));

		Cell headerCell1 = headerRow.createCell(1); // Colonne B
		headerCell1.setCellValue("Descrizione");
		headerCell1.setCellStyle(createBoldStyle(workbook, gray));

		Cell headerCell2 = headerRow.createCell(2); // Colonne C
		headerCell2.setCellValue("n° e data doc.");
		headerCell2.setCellStyle(createBoldStyle(workbook, gray));
		
		Cell headerCell3 = headerRow.createCell(3); // Colonne D
		headerCell3.setCellValue("Voce");
		headerCell3.setCellStyle(createBoldStyle(workbook, gray));

		Cell headerCell4 = headerRow.createCell(4); // Colonne E
		headerCell4.setCellValue("CASSA> ENTRATE (A)");
		headerCell4.setCellStyle(createBoldStyle(workbook, green));

		Cell headerCell5 = headerRow.createCell(5); // Colonne F
		headerCell5.setCellValue("CASSA> USCITE (B)");
		headerCell5.setCellStyle(createBoldStyle(workbook, red));

		Cell headerCell6 = headerRow.createCell(6); // Colonne G
		headerCell6.setCellValue("BANCA> ENTRATE (A)");
		headerCell6.setCellStyle(createBoldStyle(workbook, green));

		Cell headerCell7 = headerRow.createCell(7); // Colonne H
		headerCell7.setCellValue("BANCA> USCITE (B)");
		headerCell7.setCellStyle(createBoldStyle(workbook, red));

		// Ligne 8: Soldes initiaux sous les en-têtes (exemples)
		Row initialBalanceRow = sheet.createRow(currentRow++);
		for (int i = 0; i<8; i++) {
			initialBalanceRow.createCell(i).setCellStyle(createBoldStyle(workbook, orange));
		} 
		Cell descriptionInitialBalanceCell = initialBalanceRow.createCell(0);
		descriptionInitialBalanceCell.setCellValue("Residui anno precedente");
		descriptionInitialBalanceCell.setCellStyle(createBoldStyle(workbook, orange));
		Cell initialCashBalanceCell = initialBalanceRow.createCell(initialCashBalance.compareTo(BigDecimal.ZERO) > 0 ?  4 : 5); // Colonne E - F (pour CASSA)
		initialCashBalanceCell.setCellValue(initialCashBalance.doubleValue());
		initialCashBalanceCell.setCellStyle(createCurrencyStyle(workbook, orange, black));

		Cell initialBankBalanceCell = initialBalanceRow.createCell(initialBankBalance.compareTo(BigDecimal.ZERO) > 0 ? 6 : 7); // Colonne G - H (pour BANCA)
		initialBankBalanceCell.setCellValue(initialBankBalance.doubleValue());
		initialBankBalanceCell.setCellStyle(createCurrencyStyle(workbook, orange, black));
		
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

		// --- Données des transactions ---
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yy", Locale.ITALIAN);
		BigDecimal totalCashEntrate = BigDecimal.ZERO;
		BigDecimal totalCashUscite = BigDecimal.ZERO;
		BigDecimal totalBankEntrate = BigDecimal.ZERO;
		BigDecimal totalBankUscite = BigDecimal.ZERO;

		for (Transaction transaction : transactions) {
			Row dataRow = sheet.createRow(currentRow++);
			for (int i = 0; i<8; i++) {
				dataRow.createCell(i).setCellStyle(createBodyStyle(workbook, black));
			} 
			// Colonne A: Data Reg.
			Cell dateCell = dataRow.createCell(0);
			dateCell.setCellValue(transaction.getDate().format(dateFormatter));
			dateCell.setCellStyle(createBodyStyle(workbook, black));

			// Colonne B: Denominazione e DESCRIZIONE
			Cell descriptionCell = dataRow.createCell(1);
			descriptionCell.setCellValue(transaction.getDescription());
			descriptionCell.setCellStyle(createBodyStyle(workbook, black));
			
			Cell voceCell = dataRow.createCell(3); // Colonne D
			voceCell.setCellValue(transaction.getTransactionDetail() != null ? transaction.getTransactionDetail().getCode() : "");
			voceCell.setCellStyle(createBodyStyle(workbook, black));

			if (transaction.getType() == TransactionType.CASH) {
				if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					Cell docCell = dataRow.createCell(2); // Colonne C
					docCell.setCellValue(transaction.getFormattedDocumentNumber());
					docCell.setCellStyle(createBodyStyle(workbook, black));
					
					
					Cell entrateCell = dataRow.createCell(4); // Colonne E
					entrateCell.setCellValue(transaction.getAmount().doubleValue());
					entrateCell.setCellStyle(createCurrencyStyle(workbook, white, black));
					totalCashEntrate = totalCashEntrate.add(transaction.getAmount());
				} else {
					Cell usciteCell = dataRow.createCell(5); // Colonne F
					usciteCell.setCellValue(transaction.getAmount().abs().doubleValue());
					usciteCell.setCellStyle(createCurrencyStyle(workbook, white, red));
					totalCashUscite = totalCashUscite.add(transaction.getAmount().abs());
				}

			} else if (transaction.getType() == TransactionType.BANK) {
				if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
					Cell docCell = dataRow.createCell(2); // Colonne C
					docCell.setCellValue(transaction.getFormattedDocumentNumber());
					docCell.setCellStyle(createBodyStyle(workbook, black));
					
					Cell entrateCell = dataRow.createCell(6); // Colonne G
					entrateCell.setCellValue(transaction.getAmount().doubleValue());
					entrateCell.setCellStyle(createCurrencyStyle(workbook, white, black));
					totalBankEntrate = totalBankEntrate.add(transaction.getAmount());
				} else {
					Cell usciteCell = dataRow.createCell(7); // Colonne H
					usciteCell.setCellValue(transaction.getAmount().abs().doubleValue());
					usciteCell.setCellStyle(createCurrencyStyle(workbook, white, red));
					totalBankUscite = totalBankUscite.add(transaction.getAmount().abs());
				}
			}
		}

		// --- Totaux du pied de page ---

		// Ligne des valeurs totales
		Row totalRowValues = sheet.createRow(currentRow++);
		Cell totalCashIncomeValueCell = totalRowValues.createCell(4); // Colonne E
		totalCashIncomeValueCell.setCellValue(totalCashEntrate.doubleValue());
		totalCashIncomeValueCell.setCellStyle(createCurrencyStyle(workbook, green, black));
		Cell totalCashOutcomeValueCell = totalRowValues.createCell(5); // Colonne F
		totalCashOutcomeValueCell.setCellValue(totalCashUscite.doubleValue());
		totalCashOutcomeValueCell.setCellStyle(createCurrencyStyle(workbook, red, black));

		Cell totalBankIncomeValueCell = totalRowValues.createCell(6); // Colonne G
		totalBankIncomeValueCell.setCellValue(totalBankEntrate.doubleValue());
		totalBankIncomeValueCell.setCellStyle(createCurrencyStyle(workbook, green, black));
		Cell totalBankOutcomeValueCell = totalRowValues.createCell(7); // Colonne H
		totalBankOutcomeValueCell.setCellValue(totalBankUscite.doubleValue());
		totalBankOutcomeValueCell.setCellStyle(createCurrencyStyle(workbook, red, black));
		
		Row totalRowLabels = sheet.createRow(currentRow++);
		Cell totalIncomeValueCell = totalRowLabels.createCell(4); // Colonne E
		totalIncomeValueCell.setCellValue("TOT. ENTRATE");
		totalIncomeValueCell.setCellStyle(createBoldStyle(workbook, gray));
		Cell totalOutcomeValueCell = totalRowLabels.createCell(5); // Colonne F
		totalOutcomeValueCell.setCellValue("TOT. USCITE");
		totalOutcomeValueCell.setCellStyle(createBoldStyle(workbook, gray));

		Cell totalBankIncomeLabelCell = totalRowLabels.createCell(6); // Colonne E
		totalBankIncomeLabelCell.setCellValue("TOT. ENTRATE");
		totalBankIncomeLabelCell.setCellStyle(createBoldStyle(workbook, gray));
		Cell totalBankOutcomeValue = totalRowLabels.createCell(7); // Colonne F
		totalBankOutcomeValue.setCellValue("TOT. USCITE");
		totalBankOutcomeValue.setCellStyle(createBoldStyle(workbook, gray));
		
		// Ligne des soldes finaux (SALDO A-B)
		Row saldoAnnoCorrenteRow = sheet.createRow(currentRow++);
		Cell saldoCashAnnoCorrenteLabelCell = saldoAnnoCorrenteRow.createCell(4); // Colonne E
		saldoCashAnnoCorrenteLabelCell.setCellValue("SALDO (anno corrente)");
		saldoCashAnnoCorrenteLabelCell.setCellStyle(createBoldStyle(workbook, orange));
		Cell saldoCashAnnoCorrenteValueCell = saldoAnnoCorrenteRow.createCell(5); // Colonne F
		saldoCashAnnoCorrenteValueCell.setCellValue(totalCashEntrate.subtract(totalCashUscite).doubleValue());
		saldoCashAnnoCorrenteValueCell.setCellStyle(createCurrencyStyle(workbook, white, black));   
		
		Cell saldoBankAnnoCorrenteLabelCell = saldoAnnoCorrenteRow.createCell(6); // Colonne G
		saldoBankAnnoCorrenteLabelCell.setCellValue("SALDO (anno corrente)");
		saldoBankAnnoCorrenteLabelCell.setCellStyle(createBoldStyle(workbook, orange));
		Cell saldoBankAnnoCorrenteValueCell = saldoAnnoCorrenteRow.createCell(7); // Colonne H
		saldoBankAnnoCorrenteValueCell.setCellValue(totalBankEntrate.subtract(totalBankUscite).doubleValue());
		saldoBankAnnoCorrenteValueCell.setCellStyle(createCurrencyStyle(workbook, white, black)); 		

		// Ligne des soldes finaux (SALDO A-B)
		Row saldoRow = sheet.createRow(currentRow++);
		Cell saldoCashLabelCell = saldoRow.createCell(4); // Colonne E
		saldoCashLabelCell.setCellValue("SALDO (con residuo)");
		saldoCashLabelCell.setCellStyle(createBoldStyle(workbook, orange));
		Cell saldoCashValueCell = saldoRow.createCell(5); // Colonne F
		saldoCashValueCell.setCellValue(totalCashEntrate.subtract(totalCashUscite).doubleValue() + initialCashBalance.doubleValue());
		saldoCashValueCell.setCellStyle(createCurrencyStyle(workbook, white, black));   
		
		Cell saldoBankLabelCell = saldoRow.createCell(6); // Colonne G
		saldoBankLabelCell.setCellValue("SALDO (con residuo)");
		saldoBankLabelCell.setCellStyle(createBoldStyle(workbook, orange));
		Cell saldoBankValueCell = saldoRow.createCell(7); // Colonne H
		saldoBankValueCell.setCellValue(totalBankEntrate.subtract(totalBankUscite).doubleValue() + initialBankBalance.doubleValue());
		saldoBankValueCell.setCellStyle(createCurrencyStyle(workbook, white, black)); 
	}
	
	private CellStyle createBoldStyle(XSSFWorkbook workbook, XSSFColor color) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setFillForegroundColor(color);

		XSSFFont font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);

		// Bordures
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}
	
	private CellStyle createBodyStyle(XSSFWorkbook workbook, XSSFColor color) {
		XSSFCellStyle style = workbook.createCellStyle();
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setAlignment(HorizontalAlignment.LEFT);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setFillForegroundColor(white);

		XSSFFont font = workbook.createFont();
		font.setBold(false);
		font.setColor(color);
		style.setFont(font);

		// Bordures
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}
	
	private CellStyle createCurrencyStyle(XSSFWorkbook workbook, XSSFColor foregroundColor, XSSFColor color) {
		CellStyle style = createBodyStyle(workbook, color);
		style.setFillForegroundColor(foregroundColor);
		
		style.setDataFormat(workbook.createDataFormat().getFormat("€ #,##0.00")); // Format monétaire
		style.setAlignment(HorizontalAlignment.RIGHT);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}
}
