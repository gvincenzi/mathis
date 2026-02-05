package com.gist.mathis.configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.util.Assert;

import com.gist.mathis.configuration.chatmemory.InMemoryMathisChatMemoryRepository;
import com.gist.mathis.configuration.chatmemory.MathisChatMemoryObjectKeyEnum;
import com.gist.mathis.configuration.chatmemory.MathisChatMemoryRepository;

public class MathisMessageWindowChatMemory implements ChatMemory {

	private static final int DEFAULT_MAX_MESSAGES = 20;

	private final MathisChatMemoryRepository chatMemoryRepository;

	private final int maxMessages;

	private MathisMessageWindowChatMemory(MathisChatMemoryRepository chatMemoryRepository, int maxMessages) {
		Assert.notNull(chatMemoryRepository, "chatMemoryRepository cannot be null");
		Assert.isTrue(maxMessages > 0, "maxMessages must be greater than 0");
		this.chatMemoryRepository = chatMemoryRepository;
		this.maxMessages = maxMessages;
	}
	
	public void add(String conversationId, MathisChatMemoryObjectKeyEnum key, Object value) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(key, "key cannot be null");
		Assert.notNull(value, "value cannot be null");

		chatMemoryRepository.save(conversationId, key, value);
	}

	@Override
	public void add(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");

		List<Message> memoryMessages = this.chatMemoryRepository.findByConversationId(conversationId);
		List<Message> processedMessages = process(memoryMessages, messages);
		this.chatMemoryRepository.saveAll(conversationId, processedMessages);
	}

	@Override
	public List<Message> get(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		return this.chatMemoryRepository.findByConversationId(conversationId);
	}

	@Override
	public void clear(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.chatMemoryRepository.deleteByConversationId(conversationId);
	}

	private List<Message> process(List<Message> memoryMessages, List<Message> newMessages) {
		List<Message> processedMessages = new ArrayList<>();

		Set<Message> memoryMessagesSet = new HashSet<>(memoryMessages);
		boolean hasNewSystemMessage = newMessages.stream()
			.filter(SystemMessage.class::isInstance)
			.anyMatch(message -> !memoryMessagesSet.contains(message));

		memoryMessages.stream()
			.filter(message -> !(hasNewSystemMessage && message instanceof SystemMessage))
			.forEach(processedMessages::add);

		processedMessages.addAll(newMessages);

		if (processedMessages.size() <= this.maxMessages) {
			return processedMessages;
		}

		int messagesToRemove = processedMessages.size() - this.maxMessages;

		List<Message> trimmedMessages = new ArrayList<>();
		int removed = 0;
		for (Message message : processedMessages) {
			if (message instanceof SystemMessage || removed >= messagesToRemove) {
				trimmedMessages.add(message);
			}
			else {
				removed++;
			}
		}

		return trimmedMessages;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private MathisChatMemoryRepository chatMemoryRepository;

		private int maxMessages = DEFAULT_MAX_MESSAGES;

		private Builder() {
		}

		public Builder chatMemoryRepository(MathisChatMemoryRepository chatMemoryRepository) {
			this.chatMemoryRepository = chatMemoryRepository;
			return this;
		}

		public Builder maxMessages(int maxMessages) {
			this.maxMessages = maxMessages;
			return this;
		}

		public MathisMessageWindowChatMemory build() {
			if (this.chatMemoryRepository == null) {
				this.chatMemoryRepository = new InMemoryMathisChatMemoryRepository();
			}
			return new MathisMessageWindowChatMemory(this.chatMemoryRepository, this.maxMessages);
		}

	}

}
