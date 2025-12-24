package com.gist.mathis.controller.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessage {
	String conversationId;
	UserTypeEnum userType;
	String body;
}
