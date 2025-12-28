package com.gist.mathis.service;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
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
	
	@Value("classpath:/prompts/successIntentAction.st")
	private Resource successIntentActionTemplateResource;
	
	@Value("classpath:/prompts/welcome.st")
	private Resource welcomeResource;
	
	private MistralAiChatModel chatModel;
	private ChatMemory chatMemory;
	
	private ChatClient simpleChatClient;
	
	@Autowired
    public ChatService(MistralAiChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }
	
	@PostConstruct
    private void init() {
		log.info("Init chatClient via @PostConstruct");
		
		this.simpleChatClient = ChatClient.builder(chatModel)
				.defaultAdvisors(
				    	PromptChatMemoryAdvisor.builder(chatMemory).build()
				)
				.build();
	}
	
	public ChatMessage welcome(String conversationId, String language, String firstname) {	
		log.info("{} -> welcome", ChatService.class.getSimpleName());
		
		PromptTemplate welcomePromptTemplate = new PromptTemplate(this.welcomeResource);
		Prompt prompt = welcomePromptTemplate.create(Map.of("language", language, "firstname", firstname));
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}

	public IntentResponse analyzeUserMessage(ChatMessage chatMessage, String language, String firstname) {
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
		IntentResponse intentResponse = beanOutputConverter.convert(responseBody);

		return intentResponse;
	}
	
	public ChatMessage successIntentAction(String conversationId, String language, String firstname, IntentResponse intentResponse) {	
		log.info("{} -> successIntentAction", ChatService.class.getSimpleName());
		
		PromptTemplate successIntentActionTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.resource(this.successIntentActionTemplateResource)
				.build();
		Prompt prompt = successIntentActionTemplate.create(Map.of("language", language, "firstname", firstname, "ACTION", intentResponse.getIntentValue(), "DETAILS_JSON", intentResponse.getEntities()));
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
	
}
