package com.gist.mathis.controller.entity;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TransactionForm {
    private List<TransactionFormRow> transactions;
}

