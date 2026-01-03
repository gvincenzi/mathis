package com.gist.mathis.model.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "chat_id", nullable = false, unique = true, length = 20)
    private Long chatId;

    @Column(nullable = true, unique = true, length = 50)
    private String username;

    @Column(nullable = true, length = 100)
    private String firstname;
    
    @Column(nullable = true, length = 100)
    private String lastname;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @ElementCollection(targetClass = AuthorityEnum.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_authorities",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "authority")
    @Enumerated(EnumType.STRING)
    private Set<AuthorityEnum> authorities = new HashSet<AuthorityEnum>();
}
