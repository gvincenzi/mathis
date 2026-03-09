package com.gist.mathis.model.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.gist.mathis.service.MetadataMapToJsonConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class MathisMessage {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = true)
    private Set<String> recipients = new HashSet<String>();
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = true, columnDefinition = "TEXT")
    private String body;
    
    @Column(nullable = false, name = "source")
    @Enumerated(EnumType.STRING)
    private RawKnowledgeSourceEnum source;
    
    @Column(nullable = false, name = "processor")
    @Enumerated(EnumType.STRING)
    private RawKnowledgeProcessorEnum processor;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    @Convert(converter = MetadataMapToJsonConverter.class)
    private Map<String, Object> metadata = new HashMap<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "sent", nullable = false)
    private Boolean sent = Boolean.FALSE;
    
    @Column(name = "sent_at", nullable = true)
    private LocalDateTime sentAt;
}
