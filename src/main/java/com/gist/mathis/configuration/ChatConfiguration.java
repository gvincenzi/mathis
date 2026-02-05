package com.gist.mathis.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.gist.mathis.configuration.chatmemory.InMemoryMathisChatMemoryRepository;

@Configuration
public class ChatConfiguration {
	InMemoryMathisChatMemoryRepository mathisChatMemoryRepository;

	@Bean
	public MathisMessageWindowChatMemory mathisMessageWindowChatMemory() {
		return MathisMessageWindowChatMemory.builder()
				.chatMemoryRepository(mathisChatMemoryRepository)
				.maxMessages(10)
				.build();
	}

}
