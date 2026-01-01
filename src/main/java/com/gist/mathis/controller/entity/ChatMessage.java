package com.gist.mathis.controller.entity;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import lombok.Data;

@Data
public class ChatMessage {
	String conversationId;
	UserTypeEnum userType;
	String body;
	InlineKeyboardMarkup inlineKeyboardMarkup;
	
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
	
	
}
