package com.gist.mathis.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gist.mathis.configuration.chatmemory.InMemoryMathisChatMemoryRepository;

@Configuration
public class ChatConfiguration {
	@Bean
    public InMemoryMathisChatMemoryRepository mathisChatMemoryRepository() {
        return new InMemoryMathisChatMemoryRepository();
    }

	@Bean
	public MathisMessageWindowChatMemory mathisMessageWindowChatMemory(InMemoryMathisChatMemoryRepository mathisChatMemoryRepository) {
		return MathisMessageWindowChatMemory.builder()
				.chatMemoryRepository(mathisChatMemoryRepository)
				.maxMessages(10)
				.build();
	}
}
