package com.gist.mathis.controller.entity;

import org.springframework.core.io.ByteArrayResource;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import com.gist.mathis.model.entity.AuthorityEnum;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessage {
	String conversationId;
	UserTypeEnum userType;
	AuthorityEnum userAuth = AuthorityEnum.ROLE_USER;
	String body;
	InlineKeyboardMarkup inlineKeyboardMarkup;
	ByteArrayResource resource;
	
	public ChatMessage(String conversationId, UserTypeEnum userType, String body) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
	}

	public ChatMessage(String conversationId, UserTypeEnum userType, String body, InlineKeyboardMarkup inlineKeyboardMarkup) {
		this.conversationId = conversationId;
		this.userType = userType;
		this.body = body;
		this.inlineKeyboardMarkup = inlineKeyboardMarkup;
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
