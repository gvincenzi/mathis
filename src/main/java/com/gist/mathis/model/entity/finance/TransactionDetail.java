package com.gist.mathis.model.entity.finance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "transaction_detail")
public class TransactionDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 10, nullable = false)
    private String code;

    @Column(length = 255, nullable = false)
    private String description;

    @Column(length = 255, nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
	private TransactionDetailType type;
}
