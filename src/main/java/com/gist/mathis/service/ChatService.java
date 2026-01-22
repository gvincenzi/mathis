package com.gist.mathis.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.controller.entity.TransactionRequest;
import com.gist.mathis.controller.entity.UserTypeEnum;
import com.gist.mathis.model.entity.AuthorityEnum;
import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionDetailType;
import com.gist.mathis.service.entity.IntentResponse;
import com.gist.mathis.service.finance.ExcelExportService;
import com.gist.mathis.service.finance.TransactionService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

@Slf4j
@Service
public class ChatService {
	@Value("${message.ADD_TRANSACTION_TEXT}")
	private String ADD_TRANSACTION_TEXT;
	
	@Value("${message.LIST_DOCUMENTS_TEXT}")
	private String LIST_DOCUMENTS_TEXT;
	
	@Value("${message.ASK_DOCUMENTS_TEXT}")
	private String ASK_DOCUMENTS_TEXT;
	
	@Value("${prompts.adminRoleCheckFailed}")
	private Resource adminRoleCheckFailedTemplateResource;

	@Value("${prompts.analysisIntent}")
	private Resource analysisIntentTemplateResource;

	@Value("${prompts.base}")
	private Resource baseTemplateResource;

	@Value("${prompts.welcome}")
	private Resource welcomeResource;
	
	@Value("${owner.name}")
	private String ownerName;

	@Value("${owner.website}")
	private String ownerWebsite;
	
	private final KnowledgeService knowledgeService;
	private final TransactionService transactionService;
	private final ExcelExportService excelExportService;
	private final MistralAiChatModel chatModel;
	private final VectorStore vectorStore;
	private final ChatMemory chatMemory;
	
	private ChatClient chatClient, simpleChatClient;
	
	@Autowired
    public ChatService(MistralAiChatModel chatModel, VectorStore vectorStore, ChatMemory chatMemory, KnowledgeService knowledgeService, TransactionService transactionService, ExcelExportService excelExportService) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
        this.knowledgeService = knowledgeService;
        this.transactionService = transactionService;
        this.excelExportService = excelExportService;
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
			    .variables(Map.of("owner_name", ownerName, "owner_website", ownerWebsite))
			    .resource(baseTemplateResource)
			    .build();
		
		this.chatClient = ChatClient.builder(chatModel)
			    .defaultAdvisors(
			    	PromptChatMemoryAdvisor.builder(chatMemory).build(),
			        QuestionAnswerAdvisor.builder(vectorStore)
			        //.searchRequest(SearchRequest.builder().topK(10).build())
			        .promptTemplate(basePromptTemplate).build()
			    )
			    .build();
	}
	
	public ChatMessage chat(ChatMessage message) throws NumberFormatException, IOException, NoSuchElementException, JRException {
		IntentResponse intentResponse = analyzeUserMessage(message);
		ChatMessage chat = null;

		switch (intentResponse.getIntentValue()) {
			case LIST_DOCUMENTS :
				Set<Knowledge> knowledges = knowledgeService.findAll();
				chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, LIST_DOCUMENTS_TEXT, knowledges);
				break;
			case ASK_FOR_DOCUMENT : 
				Set<Knowledge> knowledgesByIntentQuery = knowledgeService.findByVectorialSearch(intentResponse);
				chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, ASK_DOCUMENTS_TEXT, knowledgesByIntentQuery);
				break;
			case ADD_TRANSACTION :
				if(AuthorityEnum.ROLE_ADMIN.equals(message.getUserAuth())) {
					BeanOutputConverter<TransactionRequest> beanOutputConverter = new BeanOutputConverter<>(TransactionRequest.class);
					TransactionRequest transactionRequest = beanOutputConverter.convert(new ObjectMapper().writeValueAsString(intentResponse.getEntities()));							
					Transaction transaction = transactionService.addTransaction(transactionRequest);
					if(TransactionDetailType.INCOME.equals(transaction.getTransactionDetail().getType())) {
						ByteArrayResource resource = new ByteArrayResource(transactionService.getTransactionReceipt(transaction.getId()));
						chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, "Receipt#" + transaction.getDocumentItemNumber() + ".pdf", resource);
					} else {
						chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, String.format(ADD_TRANSACTION_TEXT));
					}
				} else {
					chat = adminRoleCheckFailed(message.getConversationId());
				}
				break;
			case ASK_CASHFLOW :
				if(AuthorityEnum.ROLE_ADMIN.equals(message.getUserAuth())) {
					ByteArrayResource resource = new ByteArrayResource(excelExportService.generateExcelReport(Integer.parseInt(intentResponse.getEntities().get("year"))));
					chat = new ChatMessage(message.getConversationId(), UserTypeEnum.AI, "cashflow_" + intentResponse.getEntities().get("year") + ".xlsx", resource);
				} else {
					chat = adminRoleCheckFailed(message.getConversationId());
				}
				break;
			case GENERIC_QUESTION : chat = genericQuestion(message); break;
		}
		return chat;
	}
	
	public ChatMessage welcome(String conversationId, String firstname) {	
		log.info("{} -> welcome", ChatService.class.getSimpleName());
		
		PromptTemplate welcomePromptTemplate = new PromptTemplate(this.welcomeResource);
		Prompt prompt = welcomePromptTemplate.create(Map.of("firstname", firstname, "owner_name", ownerName, "owner_website", ownerWebsite));
		
		log.info(String.format("Calling MistralAI"));
		
		String responseBody = this.simpleChatClient.prompt(prompt)
			.call()
			.content();

		return new ChatMessage(conversationId, UserTypeEnum.AI, responseBody);
	}
	
	private ChatMessage genericQuestion(ChatMessage message) {
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
		IntentResponse intentResponse = beanOutputConverter.convert(responseBody.replace("```json", "").replace("`", ""));

		return intentResponse;
	}

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
