package com.gist.mathis.service.finance;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Slf4j
@Service
public class TransactionService {
	@Value("${receipt.title}") 
	private String title;
	
	@Value("${receipt.cause}") 
	String cause;
	
	@Value("${receipt.declaration}") 
	String declaration;
	
	@Value("${receipt.label_amount}") 
	String labelAmount;
	
	@Value("${receipt.label_description}") 
	String labelDescription;
	
	@Value("${receipt.label_payment_method}") 
	String labelPaymentMethod;
	
	@Value("${receipt.label_signature}") 
	String labelSignature;
	
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
		Integer lastDocumentItemNumber = transactionRepository.findLastDocumentItemNumber(transactionRequest.getDate().getYear());
		lastDocumentItemNumber = lastDocumentItemNumber == null ? 0 : lastDocumentItemNumber;
		if(transactionRequest.getAmount().compareTo(BigDecimal.ZERO)>0) transaction.setDocumentItemNumber(lastDocumentItemNumber+1);
		
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
		TransactionDetail transactionDetail = beanOutputConverter.convert(responseBody.replace("`json", "").replace("`", ""));

		return transactionDetail;
	}
	
	public List<Transaction> saveAll(List<Transaction> transactions){
		return transactionRepository.saveAll(transactions);
	}
	
	public byte[] getTransactionReceipt(Long transactionId) throws NoSuchElementException, JRException {
		Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(() -> new NoSuchElementException(String.format("Transaction with id %d does not exists",transactionId)));
		
	    InputStream reportStream = getClass().getResourceAsStream("/templates/jasper/transactionReceipt.jrxml");
	    InputStream logoStream = getClass().getResourceAsStream("/static/images/logo_responsabitaly.jpg");
	    InputStream stampStream = getClass().getResourceAsStream("/static/images/stamp_and_sign.jpg");
	    JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

	    Map<String, Object> params = new HashMap<>();
	    params.put("title", String.format(title));
	    params.put("cause", String.format(cause, transaction.getTransactionDetail().getCategory(), transaction.getTransactionDetail().getDescription()));
	    params.put("declaration", declaration);
	    params.put("label_amount", labelAmount);
	    params.put("label_description", labelDescription);
	    params.put("label_payment_method", labelPaymentMethod);
	    params.put("label_signature", labelSignature);
	    params.put("Logo", logoStream);
	    params.put("StampAndSign", stampStream);


	    JRBeanCollectionDataSource datasource = new JRBeanCollectionDataSource(List.of(transaction));
	    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, datasource);

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    JasperExportManager.exportReportToPdfStream(jasperPrint, baos);

	    return baos.toByteArray();
	}
}
