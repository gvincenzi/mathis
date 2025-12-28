package com.gist.mathis.service;

import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;
import com.gist.mathis.model.entity.Notebook;
import com.gist.mathis.model.entity.User;
import com.gist.mathis.service.entity.IntentResponse;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@Profile({ "gist" })
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final String START = "/start";
	private static final String HELP = "/help";

	@Value("${telegram.bot.username}")
	private String botUsername;

	@Value("${telegram.bot.token}")
	private String botToken;

	private final TelegramClient telegramClient;
	
	@Autowired
	private UserService userService;

	@Autowired
	private ChatService chatService;

	@Autowired
	private NotebookService notebookService;

	public TelegramBotService(@Value("${telegram.bot.token}") String botToken) {
		telegramClient = new OkHttpTelegramClient(botToken);
	}

	@Override
	public void consume(Update update) {
		log.info("Received update: message? {}, document? {}, chatId: {}", update.hasMessage(),
				update.hasMessage() && update.getMessage().hasDocument(),
				update.hasMessage() ? update.getMessage().getChatId() : null);
		if (update.hasMessage()) {
			User user = userService.findByChatId(update.getMessage().getChatId()).orElseGet(() -> {
				User newUser = new User();
				newUser.setChatId(update.getMessage().getChatId());
				newUser.setUsername(update.getMessage().getFrom().getUserName());
				newUser.setFirstname(update.getMessage().getFrom().getFirstName());
				newUser.setLastname(update.getMessage().getFrom().getLastName());
				return userService.saveUser(newUser);
			});

			if (update.getMessage().getText() != null
					&& (update.getMessage().getText().startsWith(START) || update.getMessage().getText().startsWith(HELP))) {
				String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption()
						: update.getMessage().getFrom().getLanguageCode();
				log.info("Start command detected from chatId: {}, lang: {}", update.getMessage().getChatId(), lang);
	
				try {
					ChatMessage chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
					SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
					telegramClient.execute(message);
					log.info("Welcome message sent to chatId: {}", chat.getConversationId());
				} catch (TelegramApiException e) {
					log.error("Error sending welcome message to chatId {}: {}", update.getMessage().getChatId(),
							e.getMessage(), e);
				}
			} else if (update.getMessage().getText() != null
					&& !update.getMessage().getText().startsWith(START) && !update.getMessage().getText().startsWith(HELP)) {
				log.info("Received chat message from chatId: {}, text: {}", update.getMessage().getChatId(),
						update.getMessage().getText());
				String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption()
						: update.getMessage().getFrom().getLanguageCode();
				try {
					IntentResponse intentResponse = chatService.analyzeUserMessage(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText()),lang,user.getFirstname());
						switch (intentResponse.getIntentValue()) { 
					    	case CREATE_NOTEBOOK:
					    		Notebook notebookToCreate = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseGet(() -> {
					    			Notebook notebookNew = new Notebook();
					    			notebookNew.setTitle(intentResponse.getEntities().get("notebook_title"));
					    			notebookNew.setDescription(intentResponse.getEntities().get("notebook_description"));
					    			notebookNew.setUser(user);
						    		return notebookNew;
					    		});
					    		notebookService.saveNotebook(notebookToCreate);
					            break;
					        case CREATE_NOTE:
					            //handleCreateNote(user, intentResponse.getEntities(), response);
					            break;
					        case SEARCH_NOTES:
					            //handleSearchNotes(user, intentResponse.getEntities(), response);
					            break;
					        case LIST_NOTEBOOKS:
					            //handleListNotebooks(user, response);
					            break;
					        case LIST_NOTES:
					            //handleListNotes(user, intentResponse.getEntities(), response);
					            break;
					        case DELETE_NOTEBOOK:
					        	try {
					        		Notebook notebookToDelete = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseThrow();
					        		notebookService.deleteNotebook(notebookToDelete.getNotebookId());
					        	}catch (NoSuchElementException elementException) {
					        		//FIXME Write a right message about this case
									ChatMessage chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
						        	SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
									telegramClient.execute(message);
									return;
								}
					            break;
					        case DELETE_NOTE:
					            //handleDeleteNote(user, intentResponse.getEntities(), response);
					            break;
					        default:
					        	ChatMessage chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
					        	SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
								telegramClient.execute(message);
								return;
						
						}
						
					ChatMessage chat = chatService.successIntentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(),intentResponse);
					SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
					telegramClient.execute(message);
					log.info("Chat response sent to chatId: {}", Long.toString(update.getMessage().getChatId()));
				} catch (TelegramApiException e) {
					log.error("Error processing chat message from chatId {}: {}", update.getMessage().getChatId(),
							e.getMessage(), e);
				}
			} else {
				log.warn("Received unsupported update type from chatId: {}",
						update.hasMessage() ? update.getMessage().getChatId() : null);
			}
		}
	}

	@Override
	public LongPollingUpdateConsumer getUpdatesConsumer() {
		return this;
	}
}
