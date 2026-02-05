package com.gist.mathis.configuration.chatmemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

public class InMemoryMathisChatMemoryRepository implements MathisChatMemoryRepository{

	Map<String, List<Message>> chatMemoryStore = new ConcurrentHashMap<>();
	Map<String, Map<MathisChatMemoryObjectKeyEnum,Object>> chatMemoryObjectStore = new ConcurrentHashMap<>();

	@Override
	public List<String> findConversationIds() {
		return new ArrayList<>(this.chatMemoryStore.keySet());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		List<Message> messages = this.chatMemoryStore.get(conversationId);
		return messages != null ? new ArrayList<>(messages) : List.of();
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(messages, "messages cannot be null");
		Assert.noNullElements(messages, "messages cannot contain null elements");
		this.chatMemoryStore.put(conversationId, messages);
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.chatMemoryStore.remove(conversationId);
		this.chatMemoryObjectStore.remove(conversationId);
	}
	
	public Object get(String conversationId, MathisChatMemoryObjectKeyEnum objectKey) {
		return chatMemoryObjectStore.get(conversationId).get(objectKey);
	}
	
	public void save(String conversationId, MathisChatMemoryObjectKeyEnum objectKey, Object value) {
		if(chatMemoryObjectStore.get(conversationId) == null) {
			chatMemoryObjectStore.put(conversationId,new ConcurrentHashMap<>());
		}
		chatMemoryObjectStore.get(conversationId).put(objectKey,value);
	}

}
