package com.gist.mathis.controller;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.service.ChatService;
import com.gist.mathis.service.security.MathisUserDetailsService;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/chat")
public class ChatController {
	@Autowired
	private ChatService chatService; 
	
	@Autowired
	private MathisUserDetailsService userDetailsService;
	
	@PostMapping
	public ResponseEntity<ChatMessage> chat(@RequestBody ChatMessage message, Principal principal) {
        log.info("Class [{}] Method [{}] called by {}", ChatController.class.getSimpleName(), "chat", principal.getName());
		
        MathisUser mathisUser = userDetailsService.getMathisUser(principal.getName());
		message.setUserAuth(mathisUser.getAuth());
		
		try {
			return new ResponseEntity<ChatMessage>(chatService.chat(message), HttpStatus.ACCEPTED);
		} catch (NumberFormatException | IOException | NoSuchElementException | JRException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
