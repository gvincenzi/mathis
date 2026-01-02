package com.gist.mathis.model.entity.finance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDate date;
	private String description;
	private Integer documentItemNumber;
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	private TransactionType type;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_detail_id")
    private TransactionDetail transactionDetail;
	
	public String getFormattedDocumentNumber() {
        if (this.documentItemNumber != null && this.date != null) {
            Integer year = this.date.getYear();
            return String.format("Ric. N. %d/%d", this.documentItemNumber, year);
        }
        return "";
    }

}
