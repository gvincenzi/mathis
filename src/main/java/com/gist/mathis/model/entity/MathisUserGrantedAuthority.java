package com.gist.mathis.model.entity;

import org.springframework.security.core.GrantedAuthority;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MathisUserGrantedAuthority implements GrantedAuthority{
	private static final long serialVersionUID = -8239836142917586225L;
	private String authority;
	
	public MathisUserGrantedAuthority(AuthorityEnum auth) {
		this(auth.name());
	}
}
