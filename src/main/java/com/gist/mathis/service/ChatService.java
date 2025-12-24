package com.gist.mathis.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatService {
	@Value("classpath:/prompts/base.st")
	private Resource baseTemplateResource;
	
	@Value("classpath:/prompts/welcome.st")
	private Resource welcomeResource;
	
	@Autowired
	private MistralAiChatModel chatModel;
	
	@Autowired
	private VectorStore vectorStore;
	
	@Autowired
	private ChatMemory chatMemory;
	
	private ChatClient chatClient;
	
	private PromptTemplate promptTemplate() {
		log.info(String.format("Create promptTemplate [resource: %s]", baseTemplateResource.getFilename()));
		PromptTemplate customPromptTemplate = PromptTemplate.builder()
			    .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
			    .resource(baseTemplateResource)
			    .build();
		return customPromptTemplate;
	}
	
	private ChatClient getChatClient() {
		if(this.chatClient == null) {
			log.info(String.format("Create chatClient [vectorStore: %s]", vectorStore.getName()));
			this.chatClient = ChatClient.builder(chatModel)
				    .defaultAdvisors(
				    	PromptChatMemoryAdvisor.builder(chatMemory).build(),
				        QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(this.promptTemplate()).build() // RAG advisor
				    )
				    .build();
		}
		
		return this.chatClient;
	}
	
	public ChatMessage chat(ChatMessage message) {
		log.info(String.format("%s -> %s", ChatService.class.getSimpleName(), "chat"));
		String conversationId = message.getConversationId() == null ? UUID.randomUUID().toString() : message.getConversationId();
		log.info(String.format("ChatMessage -> [%s][%s][%s]", message.getUserType().name(), conversationId, message.getBody()));
		log.info(String.format("Calling MistralAI"));
		String responseBody = getChatClient().prompt()
			// Set advisor parameters at runtime
			.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
			.user(message.getBody())
			.call()
			.content();
		log.info(String.format("MistralAI answer [%s]", responseBody.length()>15 ? responseBody.substring(0, 10)+"..." : responseBody));
		return new ChatMessage(conversationId, UserTypeEnum.HUMAN.equals(message.getUserType()) ? UserTypeEnum.AI : UserTypeEnum.HUMAN, responseBody);
	}

	public ChatMessage welcome(String conversationId, String language) {	
		log.info(String.format("%s -> %s", ChatService.class.getSimpleName(), "welcome"));
		log.info(String.format("Create chatClient [vectorStore: %s]", vectorStore.getName()));
		
		PromptTemplate welcomePromptTemplate = new PromptTemplate(this.welcomeResource);
		Prompt prompt = welcomePromptTemplate.create(Map.of("language", language));
		
		ChatClient welcomeChatClient = ChatClient.builder(chatModel)
			    .defaultAdvisors(
			        QuestionAnswerAdvisor.builder(vectorStore).build()
			    )
			    .build();
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = welcomeChatClient.prompt(prompt)
			// Set advisor parameters at runtime
			.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
}
