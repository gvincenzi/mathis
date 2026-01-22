package com.gist.mathis.controller;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.model.entity.AuthorityEnum;
import com.gist.mathis.service.ChatService;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
	@Autowired
	private ChatService chatService; 
	
	@PostMapping
	public ResponseEntity<ChatMessage> chat(@RequestBody ChatMessage message) {
		log.info(String.format("%s -> %s", ChatController.class.getSimpleName(), "chat"));
		
		/* 
		 * TODO UserAuth management (another endpoint in another controller "/api/admin/chat" with Principal as parameter)
		 * MathisUser mathisUser = userDetailsService.getMathisUser(principal.getName());
		 * message.setUserAuth(mathisUser.getAuth());
		*/
		
		message.setUserAuth(AuthorityEnum.ROLE_USER);
		
		try {
			return new ResponseEntity<ChatMessage>(chatService.chat(message), HttpStatus.ACCEPTED);
		} catch (NumberFormatException | IOException | NoSuchElementException | JRException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
