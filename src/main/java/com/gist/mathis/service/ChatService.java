package com.gist.mathis.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;
import com.gist.mathis.service.entity.IntentResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatService {
	@Value("classpath:/prompts/analysisIntent.st")
	private Resource analysisIntentTemplateResource;
	
	@Value("classpath:/prompts/base.st")
	private Resource baseTemplateResource;
	
	@Value("classpath:/prompts/welcome.st")
	private Resource welcomeResource;
	
	@Value("classpath:/prompts/ingest.st")
	private Resource ingestResource;
	
	private MistralAiChatModel chatModel;
	private VectorStore vectorStore;
	private ChatMemory chatMemory;
	
	private ChatClient chatClient, simpleChatClient;
	
	@Autowired
    public ChatService(MistralAiChatModel chatModel, VectorStore vectorStore, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }
	
	@PostConstruct
    private void init() {
		log.info("Init chatClient [vectorStore: {}] via @PostConstruct", vectorStore.getName());
		
		this.simpleChatClient = ChatClient.builder(chatModel)
				.defaultAdvisors(
				    	PromptChatMemoryAdvisor.builder(chatMemory).build()
				)
				.build();
		
		log.info("Create promptTemplate [resource: {}]", baseTemplateResource.getFilename());
		PromptTemplate basePromptTemplate = PromptTemplate.builder()
			    .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
			    .resource(baseTemplateResource)
			    .build();
		
		this.chatClient = ChatClient.builder(chatModel)
			    .defaultAdvisors(
			    	PromptChatMemoryAdvisor.builder(chatMemory).build(),
			        QuestionAnswerAdvisor.builder(vectorStore).promptTemplate(basePromptTemplate).build()
			    )
			    .build();
	}
	
	public ChatMessage chat(ChatMessage message) {
		log.info("{} -> chat", ChatService.class.getSimpleName());
		String conversationId = message.getConversationId() == null ? UUID.randomUUID().toString() : message.getConversationId();
		log.info("ChatMessage -> [{}][{}][{}]", message.getUserType().name(), conversationId, message.getBody());
		
		log.info(String.format("Calling MistralAI"));
		
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put(ChatMemory.CONVERSATION_ID, conversationId);
		String responseBody = this.chatClient.prompt()
			.advisors(advisor -> advisor.params(parameters))
			.user(message.getBody())
			.call()
			.content();
		log.info("MistralAI answer [{}]", responseBody.length() > 15 ? responseBody.substring(0, 10) + "..." : responseBody);
		return new ChatMessage(conversationId, UserTypeEnum.HUMAN.equals(message.getUserType()) ? UserTypeEnum.AI : UserTypeEnum.HUMAN, responseBody);
	}

	public ChatMessage welcome(String conversationId, String firstname) {	
		log.info("{} -> welcome", ChatService.class.getSimpleName());
		
		PromptTemplate welcomePromptTemplate = new PromptTemplate(this.welcomeResource);
		Prompt prompt = welcomePromptTemplate.create(Map.of("firstname", firstname));
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
	
	public IntentResponse analyzeUserMessage(ChatMessage chatMessage, String firstname) {
		log.info("{} -> analyzeUserMessage", ChatService.class.getSimpleName());
		
		BeanOutputConverter<IntentResponse> beanOutputConverter = new BeanOutputConverter<>(IntentResponse.class);
		String format = beanOutputConverter.getFormat();
		
		PromptTemplate analysisIntentTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.resource(this.analysisIntentTemplateResource)
				.build();
		Prompt prompt = analysisIntentTemplate.create(Map.of("format", format));	

		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.user(chatMessage.getBody())
			.call()
			.content();
		log.info("responseBody : {}",responseBody);
		IntentResponse intentResponse = beanOutputConverter.convert(responseBody.replace("`", ""));

		return intentResponse;
	}
	
	public ChatMessage ingest(String conversationId, String language, String documentName) {
		log.info("{} -> ingest", ChatService.class.getSimpleName());
		
		PromptTemplate ingestPromptTemplate = new PromptTemplate(this.ingestResource);
		Prompt prompt = ingestPromptTemplate.create(Map.of("language", language, "documentName", documentName));
			
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
}
