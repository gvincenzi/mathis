package com.gist.mathis.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

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
import com.gist.mathis.model.entity.Note;
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

	@Autowired
	private NoteService noteService;
	
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
					message.setParseMode("Markdown");
					telegramClient.execute(message);
					log.info("Welcome message sent to chatId: {}", chat.getConversationId());
				} catch (TelegramApiException e) {
					log.error("Error sending welcome message to chatId {}: {}", update.getMessage().getChatId(),
							e.getMessage(), e);
				}
			} else if (update.hasMessage() && update.getMessage().hasDocument()) {
	            String fileName = update.getMessage().getDocument().getFileName();
	            String fileId = update.getMessage().getDocument().getFileId();
	            String lang = update.getMessage().getCaption() != null ?
	                    update.getMessage().getCaption() :
	                    update.getMessage().getFrom().getLanguageCode();
	            log.info("Received document from chatId: {}, fileName: {}, fileId: {}, lang: {}",
	                    update.getMessage().getChatId(), fileName, fileId, lang);

	            try {
	                InputStreamResource inputStream = new InputStreamResource(getFile(fileId));
	                Notebook notebookToUpdate = notebookService.findByUserAndTitle(user,"Documents").orElseGet(() -> {
		    			Notebook notebookNew = new Notebook();
		    			notebookNew.setTitle("Documents");
		    			notebookNew.setDescription("Imported documents (PDF, Excel, PPT, ...");
		    			notebookNew.setUser(user);
		    			notebookNew = notebookService.saveNotebook(notebookNew);
			    		return notebookNew;
		    		});
		        	Note noteToCreate = new Note();
		        	noteToCreate.setNotebook(notebookToUpdate);
		        	noteToCreate.setTitle(fileName);
		        	noteToCreate.setContent(update.getMessage().getCaption() != null ? update.getMessage().getCaption() : update.getMessage().getText());
		        	noteService.saveNote(noteToCreate, inputStream);
		        	ChatMessage chat = chatService.successDocumentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(), fileName, notebookToUpdate.getTitle());
		            
	                SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
	                telegramClient.execute(message);
	                log.info("Ingest response sent to chatId: {}", chat.getConversationId());
	            } catch (TelegramApiException | IOException e) {
	                log.error("Error ingesting document {} from chatId {}: {}", fileName, update.getMessage().getChatId(), e.getMessage(), e);
	            }
	        } else if (update.getMessage().getText() != null
					&& !update.getMessage().getText().startsWith(START) && !update.getMessage().getText().startsWith(HELP)) {
				log.info("Received chat message from chatId: {}, text: {}", update.getMessage().getChatId(),
						update.getMessage().getText());
				String lang = update.getMessage().getCaption() != null ? update.getMessage().getCaption()
						: update.getMessage().getFrom().getLanguageCode();
				try {
					IntentResponse intentResponse = chatService.analyzeUserMessage(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText()),lang,user.getFirstname());
					ChatMessage chat = null;
					Notebook notebookToCreate = null, notebookToUpdate = null, notebookToDelete = null;
						switch (intentResponse.getIntentValue()) { 
					    	case CREATE_NOTEBOOK:
					    		notebookToCreate = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseGet(() -> {
					    			Notebook notebookNew = new Notebook();
					    			notebookNew.setTitle(intentResponse.getEntities().get("notebook_title"));
					    			notebookNew.setDescription(intentResponse.getEntities().get("notebook_description"));
					    			notebookNew.setUser(user);
						    		return notebookNew;
					    		});
					    		notebookService.saveNotebook(notebookToCreate);
					    		chat = chatService.successIntentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(),intentResponse);
					            break;
					        case CREATE_NOTE:
					        	notebookToUpdate = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseGet(() -> {
					    			Notebook notebookNew = new Notebook();
					    			notebookNew.setTitle(intentResponse.getEntities().get("notebook_title"));
					    			notebookNew.setDescription(intentResponse.getEntities().get("notebook_description"));
					    			notebookNew.setUser(user);
					    			notebookNew = notebookService.saveNotebook(notebookNew);
						    		return notebookNew;
					    		});
					        	Note noteToCreate = new Note();
					        	noteToCreate.setNotebook(notebookToUpdate);
					        	noteToCreate.setTitle(intentResponse.getEntities().get("note_title"));
					        	noteToCreate.setContent(intentResponse.getEntities().get("note_content"));
					        	noteService.saveNote(noteToCreate);
					        	chat = chatService.successIntentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(),intentResponse);
					            break;
					        case SEARCH_NOTES:
					        	chat = chatService.search(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText()), intentResponse);
					            break;
					        case SEARCH_DOCUMENTS:
					        	chat = chatService.searchInDocuments(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText()), intentResponse);
					            break;
					        case LIST_NOTEBOOKS:
					        	StringBuilder builderNotebooks = new StringBuilder();
					        	notebookService.findByUser(user).stream().forEach(nb -> builderNotebooks.append(String.format("- *%s* _%s_\n", nb.getTitle(), nb.getDescription())));
					        	chat = new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.AI, builderNotebooks.toString());
					            break;
					        case LIST_NOTES:
					        	notebookToUpdate = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseGet(() -> {
					    			Notebook notebookNew = new Notebook();
					    			notebookNew.setTitle(intentResponse.getEntities().get("notebook_title"));
					    			notebookNew.setDescription(intentResponse.getEntities().get("notebook_description"));
					    			notebookNew.setUser(user);
					    			notebookNew = notebookService.saveNotebook(notebookNew);
						    		return notebookNew;
					    		});
					        	StringBuilder builderNotes = new StringBuilder();
					        	noteService.findByNotebook(notebookToUpdate).stream().forEach(note -> builderNotes.append(String.format("- *%s* _%s_\n", note.getTitle(), note.getContent())));
					        	chat = new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.AI, builderNotes.toString());
					            break;
					        case DELETE_NOTEBOOK:
					        	try {
					        		notebookToDelete = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseThrow();
					        		notebookService.deleteNotebook(notebookToDelete.getNotebookId());
					        		chat = chatService.successIntentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(),intentResponse);
					        	}catch (NoSuchElementException elementException) {
					        		chat = chatService.noSuchElementException(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
								}
					            break;
					        case DELETE_NOTE:
					        	try {
					        		notebookToUpdate = notebookService.findByUserAndTitle(user,intentResponse.getEntities().get("notebook_title")).orElseGet(() -> {
						    			Notebook notebookNew = new Notebook();
						    			notebookNew.setTitle(intentResponse.getEntities().get("notebook_title"));
						    			notebookNew.setDescription(intentResponse.getEntities().get("notebook_description"));
						    			notebookNew.setUser(user);
						    			notebookNew = notebookService.saveNotebook(notebookNew);
							    		return notebookNew;
						    		});
					        		Note noteToDelete = noteService.findByNotebookAndTitle(notebookToUpdate,intentResponse.getEntities().get("note_title")).orElseThrow();
					        		noteService.deleteNote(noteToDelete.getNoteId());
					        		chat = chatService.successIntentAction(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname(),intentResponse);
					        	}catch (NoSuchElementException elementException) {
									chat = chatService.noSuchElementException(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
								}
					            break;
					        default:
					        	chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), lang, user.getFirstname());
						}
						
					SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
					message.setParseMode("Markdown");
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
	
	public InputStream getFile(String fileId) throws TelegramApiException, IOException {
        log.info("Fetching file from Telegram with fileId: {}", fileId);
        GetFile getFile = new GetFile(fileId);
        String filePath = telegramClient.execute(getFile).getFilePath();
        log.info("Downloading file from Telegram filePath: {}", filePath);
        return telegramClient.downloadFileAsStream(filePath);
    }
}
