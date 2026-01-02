package com.gist.mathis.service.finance;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gist.mathis.controller.entity.TransactionRequest;
import com.gist.mathis.model.entity.finance.Transaction;
import com.gist.mathis.model.entity.finance.TransactionDetail;
import com.gist.mathis.model.repository.finance.TransactionDetailRepository;
import com.gist.mathis.model.repository.finance.TransactionRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TransactionService {
	@Value("classpath:/prompts/finance/computeTransactionDetail.st")
	private Resource computeTransactionDetailTemplateResource;
	
	@Autowired
	private MistralAiChatModel chatModel;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	@Autowired
	private TransactionDetailRepository transactionDetailRepository;
	
	public Transaction addTransaction(TransactionRequest transactionRequest) throws JsonProcessingException {
		log.info("{} -> addTransaction", TransactionService.class.getSimpleName());
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		
		TransactionDetail transactionDetail = computeTransactionDetail(mapper.writeValueAsString(transactionRequest), mapper.writeValueAsString(transactionDetailRepository.findAll()));
		log.info("transactionDetail -> {}", mapper.writeValueAsString(transactionRequest));
		
		Transaction transaction = new Transaction();
		transaction.setAmount(transactionRequest.getAmount());
		transaction.setDate(transactionRequest.getDate());
		transaction.setDescription(transactionRequest.getDescription());
		transaction.setType(transactionRequest.getType());
		transaction.setDocumentItemNumber(transactionRepository.findLastDocumentItemNumber(transactionRequest.getDate().getYear())+1);
		
		transaction.setTransactionDetail(transactionDetail);
		
		return transactionRepository.save(transaction);
	}
	
	
	public TransactionDetail computeTransactionDetail(String transaction, String referential) {	
		log.info("{} -> computeTransactionDetails", TransactionService.class.getSimpleName());
		
		BeanOutputConverter<TransactionDetail> beanOutputConverter = new BeanOutputConverter<>(TransactionDetail.class);
		String format = beanOutputConverter.getFormat();
		
		PromptTemplate analysisIntentTemplate = PromptTemplate.builder()
				.renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
				.resource(this.computeTransactionDetailTemplateResource)
				.build();
		Prompt prompt = analysisIntentTemplate.create(Map.of("format",format,"transaction_details",referential,"transaction",transaction));	

		log.info(String.format("Calling MistralAI"));
		
		String responseBody = ChatClient.builder(chatModel).build()
				.prompt(prompt)
				.call()
				.content();
		log.info("responseBody : {}",responseBody);
		TransactionDetail transactionDetail = beanOutputConverter.convert(responseBody.replace("`", ""));

		return transactionDetail;
	}
}
