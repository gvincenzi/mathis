package com.gist.mathis.model.entity.membership;

import java.time.LocalDate;

import com.gist.mathis.model.entity.MathisUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "members")
public class MathisMember{
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = true)
    @JoinColumn(name = "user_id", nullable = true, unique = true)
    private MathisUser mathisUser;
    
    // First and last name
    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    // Date of birth
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    // Place of birth
    @Column(name = "place_of_birth", nullable = false, length = 100)
    private String placeOfBirth;

    // Tax code
    @Column(name = "tax_code", nullable = false, unique = true, length = 16)
    private String taxCode;

    // Residence address
    @Column(name = "residence_address", nullable = false, length = 200)
    private String residenceAddress;

    // Registration (enrollment) date
    @Column(name = "enrollment_date", nullable = false)
    private LocalDate enrollmentDate;

    // Termination date (if applicable)
    @Column(name = "termination_date")
    private LocalDate terminationDate;

    // Any positions held (e.g. President, Secretary, etc.)
    @Column(name = "positions_held", nullable = false)
    @Enumerated(EnumType.STRING)
    private MathisPositionHeldEnum positionsHeld = MathisPositionHeldEnum.SIMPLE_MEMBER;
}
