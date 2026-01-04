package com.gist.mathis.model.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.userdetails.UserDetails;

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
@Table(name = "users")
public class MathisUser implements UserDetails{
    private static final long serialVersionUID = 2081350329248648854L;

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @Column(nullable = false, unique = true, length = 20)
    private String username;
    
    @Column(nullable = false, unique = true, length = 150)
    private String password;

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
    
    @Column(name = "authority")
    @Enumerated(EnumType.STRING)
    private AuthorityEnum auth = AuthorityEnum.ROLE_USER;
    
    public Set<MathisUserGrantedAuthority> getAuthorities(){
    	Set<MathisUserGrantedAuthority> authorities = new HashSet<MathisUserGrantedAuthority>();
    	authorities.add(new MathisUserGrantedAuthority(auth));
    	return authorities;
    };
}
