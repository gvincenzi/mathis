package com.gist.mathis.configuration.chatmemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.chat.messages.Message;
import org.springframework.util.Assert;

import lombok.Data;

@Data
public class InMemoryMathisChatMemoryRepository implements MathisChatMemoryRepository{

	Map<String, ChatMemoryEntry> chatMemoryStore = new ConcurrentHashMap<>();
	Map<String, Map<MathisChatMemoryObjectKeyEnum,Object>> chatMemoryObjectStore = new ConcurrentHashMap<>();

	@Override
	public List<String> findConversationIds() {
		return new ArrayList<>(this.chatMemoryStore.keySet());
	}

	@Override
	public List<Message> findByConversationId(String conversationId) {
	    Assert.hasText(conversationId, "conversationId cannot be null or empty");
	    ChatMemoryEntry entry = this.chatMemoryStore.get(conversationId);
	    return entry != null ? entry.getMessages() : List.of();
	}

	@Override
	public void saveAll(String conversationId, List<Message> messages) {
	    Assert.hasText(conversationId, "conversationId cannot be null or empty");
	    Assert.notNull(messages, "messages cannot be null");
	    Assert.noNullElements(messages, "messages cannot contain null elements");
	    this.chatMemoryStore.put(conversationId, new ChatMemoryEntry(messages));
	}

	@Override
	public void deleteByConversationId(String conversationId) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		this.chatMemoryStore.remove(conversationId);
		this.chatMemoryObjectStore.remove(conversationId);
	}
	
	public Object get(String conversationId, MathisChatMemoryObjectKeyEnum objectKey) {
	    Map<MathisChatMemoryObjectKeyEnum, Object> map = chatMemoryObjectStore.get(conversationId);
	    return map != null ? map.get(objectKey) : null;
	}
	
	public void save(String conversationId, MathisChatMemoryObjectKeyEnum objectKey, Object value) {
	    chatMemoryObjectStore
	        .computeIfAbsent(conversationId, k -> new ConcurrentHashMap<>())
	        .put(objectKey, value);
	}

	@Override
	public Object findByConversationIdAndKey(String conversationId, MathisChatMemoryObjectKeyEnum key) {
		Assert.hasText(conversationId, "conversationId cannot be null or empty");
		Assert.notNull(key, "key cannot be null");
		Map<MathisChatMemoryObjectKeyEnum,Object> objects = this.chatMemoryObjectStore.get(conversationId);
		return objects != null ? objects.get(key) : null;
	}
}
