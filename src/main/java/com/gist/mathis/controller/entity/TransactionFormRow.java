package com.gist.mathis.controller.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransactionFormRow {
    private Long id;
    private LocalDate date;
    private String description;
    private Integer documentItemNumber;
    private Long transactionDetailId;
    private BigDecimal cassaEntrate;
    private BigDecimal cassaUscite;
    private BigDecimal bancaEntrate;
    private BigDecimal bancaUscite;
}


