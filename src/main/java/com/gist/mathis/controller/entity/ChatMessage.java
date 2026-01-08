package com.gist.mathis.controller.entity;

import java.util.Set;

import org.springframework.core.io.ByteArrayResource;

import com.gist.mathis.model.entity.AuthorityEnum;
import com.gist.mathis.model.entity.Knowledge;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
	String conversationId;
	UserTypeEnum userType;
	AuthorityEnum userAuth = AuthorityEnum.ROLE_USER;
	String body;
	Set<Knowledge> knowledges;
	ByteArrayResource resource;
	
	public ChatMessage(String conversationId, UserTypeEnum userType, String body) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
	}

	public ChatMessage(String conversationId, UserTypeEnum userType, String body, Set<Knowledge> knowledges) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
		this.knowledges = knowledges;
	}
	
	public ChatMessage(String conversationId, UserTypeEnum userType, String body, AuthorityEnum userAuth) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
		this.userAuth = userAuth;
	}

	public ChatMessage(String conversationId, UserTypeEnum userType, String body, ByteArrayResource resource) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
		this.resource = resource;
	}
	
}
