package com.gist.mathis.configuration.chatmemory;

import org.springframework.ai.chat.memory.ChatMemoryRepository;

public interface MathisChatMemoryRepository extends ChatMemoryRepository {
	void save(String conversationId, MathisChatMemoryObjectKeyEnum objectKey, Object value);
}
