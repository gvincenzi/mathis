package com.gist.mathis.service;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({ "gist" })
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final String START = "/start";
	
	@Value("${telegram.bot.username}")
	private String botUsername;

	@Value("${telegram.bot.token}")
	private String botToken;
	
	private final TelegramClient telegramClient;
	
	@Autowired
	private ChatService chatService;
	
	@Autowired
	private DocumentIngestionService documentIngestionService;

	public TelegramBotService(@Value("${telegram.bot.token}") String botToken) {
		telegramClient = new OkHttpTelegramClient(botToken);
	}

	@Override
	public void consume(Update update) {
		log.info(String.format("%s -> %s", TelegramBotService.class.getSimpleName(), "consume"));
		if (update.hasMessage() && update.getMessage().getText() != null && update.getMessage().getText().startsWith(START)) {
			String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption() : update.getMessage().getFrom().getLanguageCode();
			try {
				ChatMessage chat = chatService.welcome(Long.toString(update.getMessage().getChatId()),lang);
				SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
				telegramClient.execute(message);
			} catch (TelegramApiException e) {
				log.error(e.getMessage());
			}
		} else if (update.hasMessage() && update.getMessage().getText() != null && !update.getMessage().getText().startsWith(START)) {
			try {
				ChatMessage chat = chatService.chat(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText()));
				SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
				telegramClient.execute(message);
			} catch (TelegramApiException e) {
				log.error(e.getMessage());
			}
		} else if (update.hasMessage() && update.getMessage().hasDocument()) {
			try {
				String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption() : update.getMessage().getFrom().getLanguageCode();
				InputStreamResource inputStream = new InputStreamResource(getFile(update.getMessage().getDocument().getFileId()));
				documentIngestionService.ingest(inputStream);
				
				ChatMessage chat = chatService.ingest(Long.toString(update.getMessage().getChatId()),lang,update.getMessage().getDocument().getFileName());
				SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
				telegramClient.execute(message);
			} catch (TelegramApiException | IOException  e) {
				log.error(e.getMessage());
			}
		}
	}

	@Override
	public LongPollingUpdateConsumer getUpdatesConsumer() {
		return this;
	}
	
	public InputStream getFile(String fileId) throws TelegramApiException, IOException {
		GetFile getFile = new GetFile(fileId);
		String filePath = telegramClient.execute(getFile).getFilePath();
		return telegramClient.downloadFileAsStream(filePath);
	}
}
