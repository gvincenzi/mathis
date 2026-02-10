package com.gist.mathis.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.service.ChatService;
import com.gist.mathis.service.TelegramBotService;
import com.gist.mathis.service.security.MathisUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
	@Autowired
	private ChatService chatService; 
	
	@Autowired
	private MathisUserDetailsService userDetailsService;
	
	@Autowired
	private Optional<TelegramBotService> telegramBotService;
	
	@PostMapping
	public ResponseEntity<ChatMessage> chat(@RequestBody ChatMessage message, Principal principal) {
        log.info("Class [{}] Method [{}] called by {}", ChatController.class.getSimpleName(), "chat", principal.getName());
		
        MathisUser mathisUser = userDetailsService.getMathisUser(principal.getName());
		message.setUserAuth(mathisUser.getAuth());
		
		try {
			ChatMessage chatMessage = chatService.chat(message);
			
			//Send notification to admin
			if(chatMessage.getNotificationMessageForAdmin() != null && telegramBotService.isPresent()) {
				List<MathisUser> admins = userDetailsService.findAdmins();
				admins.stream().forEach(admin -> 
				{
					SendMessage messageToAdmin = new SendMessage(admin.getUsername(), chatMessage.getNotificationMessageForAdmin());
					messageToAdmin.setParseMode("Markdown");
					try {
						telegramBotService.get().getTelegramClient().execute(messageToAdmin);
					} catch (TelegramApiException e) {
						log.error("Error processing chat message from chatId {}: {}", admin.getUsername(), e.getMessage(), e);
					}
				});
			}
			
			return new ResponseEntity<ChatMessage>(chatMessage, HttpStatus.ACCEPTED);
		} catch (NumberFormatException | IOException e) {
			log.error(e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
