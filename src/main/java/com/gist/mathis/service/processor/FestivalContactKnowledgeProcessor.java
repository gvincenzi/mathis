package com.gist.mathis.service.processor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeProcessorEnum;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.model.repository.MathisMessageRepository;
import com.gist.mathis.service.RawKnowledgeService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ProcessorScheduler(configKey = "mathis.processors.festival-contact")
@Component
public class FestivalContactKnowledgeProcessor implements KnowledgeProcessor {
	@Autowired
	private RawKnowledgeService rawKnowledgeService;
	
	@Autowired
	private MathisMessageRepository mathisMessageRepository;

	@Value("${prompts.festivalContact}")
	private Resource festivalContactPromptTemplateResource;

	@Value("${owner.name}")
	private String ownerName;

	@Value("${owner.website}")
	private String ownerWebsite;
	
	private final MistralAiChatModel chatModel;
	private final VectorStore vectorStore;
	private PromptTemplate festivalContactPromptTemplate;
	private ChatClient chatClient;

	@Autowired
	public FestivalContactKnowledgeProcessor(MistralAiChatModel chatModel, VectorStore vectorStore) {
		this.chatModel = chatModel;
		this.vectorStore = vectorStore;
	}
	
	@Override
	public RawKnowledgeProcessorEnum getProcessorName() {
		return RawKnowledgeProcessorEnum.FESTIVAL_FIRST_CONTACT;
	}

	@PostConstruct
	private void init() {
		log.info("Init chatClient [vectorStore: {}] via @PostConstruct", vectorStore.getName());
		log.info("Create festivalContactPromptTemplate [resource: {}]",
				festivalContactPromptTemplateResource.getFilename());
		festivalContactPromptTemplate = new PromptTemplate(this.festivalContactPromptTemplateResource);
		this.chatClient = ChatClient.builder(chatModel).build();
	}

	@Override
	public void process() throws InterruptedException {
		log.info("[{}][{}] Start processing",getProcessorName(),getClass().getSimpleName());
		List<RawKnowledge> list = rawKnowledgeService.findBySourceAndProcessedByIsNull(RawKnowledgeSourceEnum.FESTIVAL);
		for (RawKnowledge festival : list) {
			String festival_name = (String) festival.getMetadata().get("name");
			String country = (String) festival.getMetadata().get("country");
			String festival_mail = (String) festival.getMetadata().get("email");
			String festival_description = festival.getDescription();
			
			MathisMessage mathisMessage;
			String title = String.format("[%s] – Proposal for %s", ownerName, festival_name);
			Optional<MathisMessage> byName = mathisMessageRepository.findByTitleAndProcessor(title, getProcessorName());
			if(byName.isEmpty()) {
				mathisMessage = new MathisMessage();
				mathisMessage.setProcessor(getProcessorName());
				mathisMessage.setTitle(title);
				mathisMessage.setSource(RawKnowledgeSourceEnum.FESTIVAL);
			} else {
				mathisMessage = byName.get();
				
				if(mathisMessage.getSent()) {
					log.info("Message already sent [%s]", mathisMessage.getSentAt().toString());
					continue;
				}
				
				mathisMessage.getRecipients().clear();
				mathisMessage.getMetadata().clear();
				festival.setUpdatedAt(null);
			}
			
			mathisMessage.getMetadata().put("country", country);
			mathisMessage.getRecipients().add(festival_mail);
			
			Map<String, Object> templateVars = Map.of("owner_name", ownerName, "owner_website", ownerWebsite,
					"festival_name", festival_name, "festival_mail", festival_mail, "festival_description", festival_description, "country", country);
			Prompt prompt = festivalContactPromptTemplate.create(templateVars);
			
			String userQuery = "You are an assistant who writes introduction emails for the musical artist {owner_name}.\r\n"
					+ "Given the name of a festival ({festival_name}), the country where it is held ({country}), the festival's contact email ({festival_mail}), and a description of the festival ({festival_description}), you must write a formal email to the festival organizers to propose the artist for one of the upcoming editions.\r\n"
					+ "\r\n"
					+ "**Important Note on Information Usage:** While the core content about the artist's career, awards, collaborations, musical style, and recent projects must *exclusively* come from the provided Context Information, the following specific media links must *always* be included in a dedicated 'Media' section, regardless of their presence in the Context Information.\r\n"
					.replace("{owner_name}", ownerName).replace("{festival_name}", festival_name).replace("{country}", country).replace("{festival_mail}", festival_mail).replace("{festival_description}", festival_description);
			
			String mailBody = this.chatClient.prompt(prompt)
					.advisors(
							QuestionAnswerAdvisor.builder(vectorStore)
							.build())
					.user(userQuery)
					.call()
					.content().replace("```html", "").replace("```", "");
			
			mathisMessage.setBody(mailBody);
			mathisMessage = mathisMessageRepository.save(mathisMessage);
			
			festival.setProcessedBy(getProcessorName());
			festival.setProcessedAt(LocalDateTime.now());
			rawKnowledgeService.updateRawKnowledge(festival);
			
			log.info(String.format("Festival Contact Mail generated: %d > %s", mathisMessage.getId(), mathisMessage.getTitle()));
			
			Thread.sleep(1000);
		}
		
		log.info("[{}][{}] End processing",getProcessorName(),getClass().getSimpleName());
	}
}
