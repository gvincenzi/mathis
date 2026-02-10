package com.gist.mathis.configuration.chatmemory;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.Message;

import lombok.Data;

@Data
public class ChatMemoryEntry {
    private final List<Message> messages;
    private volatile long lastAccess;

    public ChatMemoryEntry(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
        touch();
    }

    public List<Message> getMessages() {
        this.lastAccess = System.currentTimeMillis();
        return new ArrayList<>(messages);
    }

    public void setMessages(List<Message> newMessages) {
        this.messages.clear();
        this.messages.addAll(newMessages);
        touch();
    }

    public void touch() {
        this.lastAccess = System.currentTimeMillis();
    }
}

