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
        log.info("Received update: message? {}, document? {}, chatId: {}",
                update.hasMessage(),
                update.hasMessage() && update.getMessage().hasDocument(),
                update.hasMessage() ? update.getMessage().getChatId() : null);

        if (update.hasMessage() && update.getMessage().getText() != null && update.getMessage().getText().startsWith(START)) {
            String lang = update.getMessage().getCaption() != null ?
                    update.getMessage().getCaption() :
                    update.getMessage().getFrom().getLanguageCode();
            log.info("Start command detected from chatId: {}, lang: {}", update.getMessage().getChatId(), lang);

            try {
                ChatMessage chat = chatService.welcome(Long.toString(update.getMessage().getChatId()), lang);
                SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
                telegramClient.execute(message);
                log.info("Welcome message sent to chatId: {}", chat.getConversationId());
            } catch (TelegramApiException e) {
                log.error("Error sending welcome message to chatId {}: {}", update.getMessage().getChatId(), e.getMessage(), e);
            }
        } else if (update.hasMessage() && update.getMessage().getText() != null && !update.getMessage().getText().startsWith(START)) {
            log.info("Received chat message from chatId: {}, text: {}", update.getMessage().getChatId(), update.getMessage().getText());
            try {
                ChatMessage chat = chatService.chat(new ChatMessage(
                        Long.toString(update.getMessage().getChatId()),
                        UserTypeEnum.HUMAN,
                        update.getMessage().getText()));
                SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
                telegramClient.execute(message);
                log.info("Chat response sent to chatId: {}", chat.getConversationId());
            } catch (TelegramApiException e) {
                log.error("Error processing chat message from chatId {}: {}", update.getMessage().getChatId(), e.getMessage(), e);
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
                documentIngestionService.ingest(inputStream);
                log.info("Document {} ingested successfully for chatId: {}", fileName, update.getMessage().getChatId());

                ChatMessage chat = chatService.ingest(Long.toString(update.getMessage().getChatId()), lang, fileName);
                SendMessage message = new SendMessage(chat.getConversationId(), chat.getBody());
                telegramClient.execute(message);
                log.info("Ingest response sent to chatId: {}", chat.getConversationId());
            } catch (TelegramApiException | IOException e) {
                log.error("Error ingesting document {} from chatId {}: {}", fileName, update.getMessage().getChatId(), e.getMessage(), e);
            }
        } else {
            log.warn("Received unsupported update type from chatId: {}", 
                update.hasMessage() ? update.getMessage().getChatId() : null);
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
