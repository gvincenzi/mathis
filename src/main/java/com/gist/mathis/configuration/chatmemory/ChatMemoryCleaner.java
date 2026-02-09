package com.gist.mathis.configuration.chatmemory;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.gist.mathis.configuration.MathisChatMemoryProperties;

@Component
@EnableScheduling
@EnableConfigurationProperties(MathisChatMemoryProperties.class)
public class ChatMemoryCleaner {
	private final MathisChatMemoryProperties properties;
	private final InMemoryMathisChatMemoryRepository mathisChatMemoryRepository;
	
	public ChatMemoryCleaner(MathisChatMemoryProperties properties, InMemoryMathisChatMemoryRepository mathisChatMemoryRepository) {
		this.properties = properties;
		this.mathisChatMemoryRepository = mathisChatMemoryRepository;
	}
	
	
	@Scheduled(cron = "${mathis.chat.memory.cleanup-cron}")
	public void cleanUpExpiredConversations() {
		long now = System.currentTimeMillis();
		mathisChatMemoryRepository.getChatMemoryStore().entrySet().removeIf(entry ->
	        now - entry.getValue().getLastAccess() > properties.getExpirationMillis()
	    );
		mathisChatMemoryRepository.getChatMemoryObjectStore().keySet().removeIf(conversationId -> !mathisChatMemoryRepository.getChatMemoryStore().containsKey(conversationId));
	}
}
