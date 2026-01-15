package com.gist.mathis.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;
import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.service.security.MathisUserDetailsService;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Service
@ConditionalOnBooleanProperty(name = "telegram.bot.active", havingValue = true, matchIfMissing = false)
public class TelegramBotService implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
	private static final String START = "/start";

	@Value("${telegram.bot.username}")
	private String botUsername;

	@Value("${telegram.bot.token}")
	private String botToken;

	private final TelegramClient telegramClient;

	@Autowired
	private MathisUserDetailsService userService;

	@Autowired
	private ChatService chatService;
	
	@Autowired
	private KnowledgeService knowledgeService;
	
	public TelegramBotService(@Value("${telegram.bot.token}") String botToken) {
		telegramClient = new OkHttpTelegramClient(botToken);
	}

	@Override
	public void consume(Update update) {
		log.info("Received update: message? {}, document? {}, chatId: {}", update.hasMessage(),
				update.hasMessage() && update.getMessage().hasDocument(),
				update.hasMessage() ? update.getMessage().getChatId() : null);
		
		ChatMessage chat;
		
		if (update.hasMessage() || update.hasCallbackQuery()) {
			Long chatId = update.hasMessage() ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();
			MathisUser user = userService.findOrCreateByTelegram(update,chatId);

			if (update.hasMessage() && update.getMessage().getText() != null && update.getMessage().getText().startsWith(START)) {
				log.info("Start command detected from chatId: {}", update.getMessage().getChatId());

				try {
					chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), user.getFirstname());
					SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
					message.setParseMode("Markdown");
					telegramClient.execute(message);
					log.info("Welcome message sent to chatId: {}", chat.getConversationId());
				} catch (TelegramApiException e) {
					log.error("Error sending welcome message to chatId {}: {}", update.getMessage().getChatId(), e.getMessage(), e);
				}
			} else if (update.hasMessage() && update.getMessage().getText() != null && !update.getMessage().getText().startsWith(START)) {
				log.info("Received chat message from chatId: {}, text: {}", update.getMessage().getChatId(), update.getMessage().getText());
				try {
					chat = chatService.chat(new ChatMessage(Long.toString(update.getMessage().getChatId()), UserTypeEnum.HUMAN, update.getMessage().getText(), user.getAuth()));
					if(chat.getResource() != null) {
						SendDocument doc = new SendDocument(botToken, new InputFile(chat.getResource().getInputStream(), chat.getBody()));
						doc.setChatId(chatId);
						telegramClient.execute(doc);
					} else {
						SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
						message.setParseMode("Markdown");
						if(chat.getKnowledges() != null && !chat.getKnowledges().isEmpty()) {
							message.setReplyMarkup(getInlineKeyboard(chat.getKnowledges()));
						}
						telegramClient.execute(message);
					}
					log.info("Chat response sent to chatId: {}", chat.getConversationId());
				} catch (TelegramApiException | IOException | NumberFormatException e) {
					log.error("Error processing chat message from chatId {}: {}", update.getMessage().getChatId(), e.getMessage(), e);
				}
			} else if (update.hasCallbackQuery()) {
				CallbackQuery callbackQuery = update.getCallbackQuery();
				SendMessage message = handleCallbackQuery(callbackQuery.getData(), callbackQuery.getMessage().getChatId(), user);
				try {
					telegramClient.execute(message);
				} catch (TelegramApiException e) {
					log.error(e.getMessage());
				}
			} else {
				log.warn("Received unsupported update type from chatId: {}", update.hasMessage() ? update.getMessage().getChatId() : null);
			}
		}
	}

	@Override
	public LongPollingUpdateConsumer getUpdatesConsumer() {
		return this;
	}
	
	private SendMessage handleCallbackQuery(String callbackData, Long chatId, MathisUser user) {
		SendMessage message = null;
		String callBackAction = callbackData.substring(0,callbackData.indexOf("#"));
		switch (callBackAction) {
			case "knowledge":
				Optional<Knowledge> knowledge = knowledgeService.findById(Long.parseLong(callbackData.substring(callbackData.indexOf("#")+1)));
				if(knowledge.isPresent())
					message = new SendMessage(Long.toString(chatId), String.format("%s\n%s",knowledge.get().getDescription(),knowledge.get().getUrl()));
				break;
			default: 
				break;
				
		}
		
		if(message == null) {
			ChatMessage chat = chatService.welcome(Long.toString(chatId), user.getFirstname());
			message = new SendMessage(chat.getConversationId(), chat.getBody());
		}
		
		return message;
		
	}
	
	private InlineKeyboardMarkup getInlineKeyboard(Collection<Knowledge> knowledges) {
		List<InlineKeyboardRow> inlineKeyboardRows = new ArrayList<>(knowledges.size());
		
		knowledges.forEach(k -> {
			List<InlineKeyboardButton> rowInline = new ArrayList<>();
			InlineKeyboardButton kBtn = new InlineKeyboardButton(String.format("%s",k.getTitle()));
			kBtn.setCallbackData(String.format("knowledge#%d", k.getKnowledgeId()));
			rowInline.add(kBtn);
			inlineKeyboardRows.add(new InlineKeyboardRow(rowInline));
		});
		
		return new InlineKeyboardMarkup(inlineKeyboardRows);
	}
}
