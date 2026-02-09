package com.gist.mathis.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.MessageType;
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

import com.gist.mathis.configuration.MathisMessageWindowChatMemory;
import com.gist.mathis.configuration.chatmemory.MathisChatMemoryObjectKeyEnum;
import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.UserTypeEnum;
import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.service.entity.IntentResponse;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatService {
	@Value("${message.WELCOME_TEXT}")
	private String WELCOME_TEXT;
	
	@Value("${message.LIST_DOCUMENTS_TEXT}")
	private String LIST_DOCUMENTS_TEXT;
	
	@Value("${message.ASK_DOCUMENTS_TEXT}")
	private String ASK_DOCUMENTS_TEXT;
	
	@Value("${message.USER_MAIL_SENT_TEXT}")
	private String USER_MAIL_SENT_TEXT;
	
	@Value("${message.ASK_USER_MAIL_TEXT}")
	private String ASK_USER_MAIL_TEXT;
	
	@Value("${prompts.adminNotification}")
	private Resource adminNotificationTemplateResource;
	
	@Value("${prompts.adminRoleCheckFailed}")
	private Resource adminRoleCheckFailedTemplateResource;

	@Value("${prompts.analysisIntent}")
	private Resource analysisIntentTemplateResource;

	@Value("${prompts.base}")
	private Resource baseTemplateResource;

	@Value("${prompts.translation}")
	private Resource translationResource;
	
	@Value("${owner.name}")
	private String ownerName;

	@Value("${owner.website}")
	private String ownerWebsite;
	
	private final KnowledgeService knowledgeService;
	private final MistralAiChatModel chatModel;
	private final VectorStore vectorStore;
	private final MathisMessageWindowChatMemory mathisMessageWindowChatMemory;
	
	private ChatClient chatClient, simpleChatClient;
	
	@Autowired
    public ChatService(MistralAiChatModel chatModel, VectorStore vectorStore, MathisMessageWindowChatMemory mathisMessageWindowChatMemory, KnowledgeService knowledgeService) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.mathisMessageWindowChatMemory = mathisMessageWindowChatMemory;
        this.knowledgeService = knowledgeService;
    }
	
	@PostConstruct
    private void init() {
		log.info("Init chatClient [vectorStore: {}] via @PostConstruct", vectorStore.getName());
		
		this.simpleChatClient = ChatClient.builder(chatModel)
				.defaultAdvisors(
				    	PromptChatMemoryAdvisor.builder(mathisMessageWindowChatMemory).build()
				)
				.build();
		
		log.info("Create promptTemplate [resource: {}]", baseTemplateResource.getFilename());
		PromptTemplate basePromptTemplate = PromptTemplate.builder()
			    .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
			    .variables(Map.of("owner_name", ownerName, "owner_website", ownerWebsite))
			    .resource(baseTemplateResource)
			    .build();
		
		this.chatClient = ChatClient.builder(chatModel)
			    .defaultAdvisors(
			    	PromptChatMemoryAdvisor.builder(mathisMessageWindowChatMemory).build(),
			        QuestionAnswerAdvisor.builder(vectorStore)
			        //.searchRequest(SearchRequest.builder().topK(10).build())
			        .promptTemplate(basePromptTemplate).build()
			    )
			    .build();
	}
	
	public ChatMessage chat(ChatMessage message) throws NumberFormatException, IOException {
		IntentResponse intentResponse = analyzeUserMessage(message);
		ChatMessage chat = null;

		switch (intentResponse.getIntentValue()) {
			case LIST_DOCUMENTS :
				Set<Knowledge> knowledges = knowledgeService.findAll();
				chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, translation(LIST_DOCUMENTS_TEXT,message.getConversationId()), knowledges);
				break;
			case ASK_FOR_DOCUMENT : 
				Set<Knowledge> knowledgesByIntentQuery = knowledgeService.findByVectorialSearch(intentResponse);
				chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, translation(ASK_DOCUMENTS_TEXT,message.getConversationId()), knowledgesByIntentQuery);
				break;
			case USER_MAIL_SENT :
				String userMail = intentResponse.getEntities().get("email");
				mathisMessageWindowChatMemory.add(message.getConversationId(), MathisChatMemoryObjectKeyEnum.USER_MAIL, userMail);
				log.info("eMail [{}] saved in conversationId [{}]",userMail,message.getConversationId());
				chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, translation(USER_MAIL_SENT_TEXT,message.getConversationId()));
				break;
			case NOTIFY_ADMIN :
				if(intentResponse.getEntities().containsKey("email") && intentResponse.getEntities().get("email") != null && intentResponse.getEntities().get("email") !="") {
					String email = intentResponse.getEntities().get("email");
					mathisMessageWindowChatMemory.add(message.getConversationId(), MathisChatMemoryObjectKeyEnum.USER_MAIL, email);
					log.info("eMail [{}] saved in conversationId [{}]",email,message.getConversationId());
				}
				
				if(mathisMessageWindowChatMemory.get(message.getConversationId(),MathisChatMemoryObjectKeyEnum.USER_MAIL) == null) {
					chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, translation(ASK_USER_MAIL_TEXT,message.getConversationId()));
					break;
				} else {
					String userMessageForAdmin = intentResponse.getEntities().get("message");
					log.info("Sending message to admins [{}]",userMessageForAdmin);
					String conversationForSummary = conversationHistory(message.getConversationId());

					log.info("Create adminNotificationPromptTemplate [resource: {}]", adminNotificationTemplateResource.getFilename());
					PromptTemplate adminNotificationTemplate = new PromptTemplate(this.adminNotificationTemplateResource);
					Prompt prompt = adminNotificationTemplate.create(Map.of("owner_name", ownerName, "conversationForSummary", conversationForSummary, "userMessageForAdmin", userMessageForAdmin));
					String messageToAdmin = this.simpleChatClient.prompt(prompt)
							.call()
							.content();
					
					chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, translation("Message correctly sent \uD83D\uDC4D",message.getConversationId()), messageToAdmin);
					break;
				}
			case GENERIC_QUESTION : chat = genericQuestion(message); break;
		}
		return chat;
	}

	private String conversationHistory(String conversationId) {
		String conversationForSummary = mathisMessageWindowChatMemory.get(conversationId).stream()
		        .map(m -> (m.getMessageType() == MessageType.USER ? UserTypeEnum.HUMAN : UserTypeEnum.AI) + ": " + m.getText())
		        .collect(Collectors.joining("\n"));
		return conversationForSummary;
	}
	
	public ChatMessage welcome(String conversationId, String firstname) {	
		log.info("{} -> welcome", ChatService.class.getSimpleName());
		return new ChatMessage(conversationId, UserTypeEnum.AI, translation(WELCOME_TEXT.replace("{firstname}", firstname).replace("{owner_website}", ownerWebsite).replace("{owner_name}", ownerName), conversationId));
	}
	
	public String translation(String toTranslate, String conversationId) {	
		log.info("{} -> translation", ChatService.class.getSimpleName());
		
		PromptTemplate translationTemplate = new PromptTemplate(this.translationResource);
		Prompt prompt = translationTemplate.create(Map.of("conversationForSummary", conversationHistory(conversationId)));
		
		log.info(String.format("Calling MistralAI"));
		
		return this.simpleChatClient.prompt(prompt).user(toTranslate)
			.call()
			.content();
	}
	
	private ChatMessage genericQuestion(ChatMessage message) {
		log.info("{} -> chat", ChatService.class.getSimpleName());
		String conversationId = message.getConversationId() == null ? UUID.randomUUID().toString() : message.getConversationId();
		log.info("ChatMessage -> [{}][{}][{}]", message.getUserType().name(), conversationId, message.getBody());
		
		log.info(String.format("Calling MistralAI"));
		
		mathisMessageWindowChatMemory.add(message.getConversationId(), MathisChatMemoryObjectKeyEnum.USER_ROLE, message.getUserAuth());
		
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
	
	private IntentResponse analyzeUserMessage(ChatMessage chatMessage) {
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

	@SuppressWarnings("unused")
	private ChatMessage adminRoleCheckFailed(String conversationId) {
		log.info("{} -> adminRoleCheckFailed", ChatService.class.getSimpleName());
		
		PromptTemplate adminRoleCheckFailedPromptTemplate = new PromptTemplate(this.adminRoleCheckFailedTemplateResource);
		Prompt prompt = adminRoleCheckFailedPromptTemplate.create();
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
}
