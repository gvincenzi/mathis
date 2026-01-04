package com.gist.mathis.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.service.ChatService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
	@Autowired
	private ChatService chatService; 
	
	@PostMapping
	public ResponseEntity<ChatMessage> chat(@RequestBody ChatMessage message) {
		log.info(String.format("%s -> %s", ChatController.class.getSimpleName(), "chat"));
		try {
			return new ResponseEntity<ChatMessage>(chatService.chat(message), HttpStatus.ACCEPTED);
		} catch (NumberFormatException | IOException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
