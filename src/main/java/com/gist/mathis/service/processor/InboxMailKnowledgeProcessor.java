package com.gist.mathis.service.processor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gist.mathis.controller.entity.ChatMessage;
import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeProcessorEnum;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.model.repository.MathisMessageRepository;
import com.gist.mathis.service.ChatService;
import com.gist.mathis.service.RawKnowledgeService;
import com.gist.mathis.service.TelegramBotService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ProcessorScheduler(configKey = "mathis.processors.inboxmail")
@Component
public class InboxMailKnowledgeProcessor implements KnowledgeProcessor {
	@Autowired
	private RawKnowledgeService rawKnowledgeService;
	
	@Autowired
	private MathisMessageRepository mathisMessageRepository;
	
	@Autowired
	private ChatService chatService;
	
	@Autowired
	private TelegramBotService telegramBotService;
	
	@Value("${spring.mail.cc}")
    private String ownerMail;

	@Autowired
	public InboxMailKnowledgeProcessor() {
	}
	
	@Override
	public RawKnowledgeProcessorEnum getProcessorName() {
		return RawKnowledgeProcessorEnum.INBOX_MAIL;
	}

	@PostConstruct
	private void init() {
	}

	@Override
	public void process() throws InterruptedException {
		log.debug("[{}][{}] Start processing",getProcessorName(),getClass().getSimpleName());
		List<RawKnowledge> list = rawKnowledgeService.findBySourceAndProcessedByIsNull(RawKnowledgeSourceEnum.INBOX_MAIL);
		for (RawKnowledge inboxMail : list) {
			String subject = (String) inboxMail.getMetadata().get("subject");
			String from = (String) inboxMail.getMetadata().get("from");
			String sentDate = (String) inboxMail.getMetadata().get("sentDate");
			String cc = (String) inboxMail.getMetadata().get("cc");
			String bcc = (String) inboxMail.getMetadata().get("bcc");
			
			MathisMessage mathisMessage;
			Optional<MathisMessage> byName = mathisMessageRepository.findByTitleAndProcessor(inboxMail.getName(), getProcessorName());
			if(byName.isEmpty()) {
				mathisMessage = new MathisMessage();
				mathisMessage.setProcessor(getProcessorName());
				mathisMessage.setTitle(inboxMail.getName());
				mathisMessage.setSource(RawKnowledgeSourceEnum.INBOX_MAIL);
			} else {
				mathisMessage = byName.get();
				
				if(mathisMessage.getSent()) {
					log.info("Message already sent [%s]", mathisMessage.getSentAt().toString());
					continue;
				}
				
				mathisMessage.getRecipients().clear();
				mathisMessage.getMetadata().clear();
				inboxMail.setUpdatedAt(null);
			}
			
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
		        Date date = sdf.parse(sentDate);
				mathisMessage.setCreatedAt(date.toInstant());
			} catch (ParseException e) {
				log.error(e.getMessage());
			}
	       
			
			ChatMessage mailReceivedToAdmin = chatService.mailReceivedToAdmin(ProcessorUtil.cleanText(inboxMail.getDescription()));
			mathisMessage.getMetadata().put("subject", subject);
			mathisMessage.getMetadata().put("from", from);
			mathisMessage.getMetadata().put("sentDate", sentDate);
			mathisMessage.getMetadata().put("cc", cc);
			mathisMessage.getMetadata().put("bcc", bcc);
			mathisMessage.getMetadata().put("body", inboxMail.getDescription());
			
			mathisMessage.setBody(mailReceivedToAdmin.getNotificationMessageForAdmin());
			mathisMessage.getRecipients().add(ownerMail);
			
			telegramBotService.sendNotificationMessageForAdmin(mailReceivedToAdmin);
			
			mathisMessage = mathisMessageRepository.save(mathisMessage);
			
			inboxMail.setProcessedBy(getProcessorName());
			inboxMail.setProcessedAt(LocalDateTime.now());
			rawKnowledgeService.updateRawKnowledge(inboxMail);
			
			log.info(String.format("Inbox Mail generated: %d > %s", mathisMessage.getId(), mathisMessage.getTitle()));
			
			Thread.sleep(1000);
		}
		
		log.debug("[{}][{}] End processing",getProcessorName(),getClass().getSimpleName());
	}
}
