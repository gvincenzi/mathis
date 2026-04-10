package com.gist.mathis.service.processor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import reactor.core.publisher.Flux;

@Slf4j
@ProcessorScheduler(configKey = "mathis.processors.organizer-contact")
@Component
public class OrganizerContactKnowledgeProcessor implements KnowledgeProcessor {
	@Autowired
	private RawKnowledgeService rawKnowledgeService;
	
	@Autowired
	private MathisMessageRepository mathisMessageRepository;

	@Value("${prompts.organizerContact}")
	private Resource organizerContactPromptTemplateResource;

	@Value("${owner.name}")
	private String ownerName;

	@Value("${owner.website}")
	private String ownerWebsite;
	
	private final MistralAiChatModel chatModel;
	private final VectorStore vectorStore;
	private PromptTemplate organizerContactPromptTemplate;
	private ChatClient chatClient;

	@Autowired
	public OrganizerContactKnowledgeProcessor(MistralAiChatModel chatModel, VectorStore vectorStore) {
		this.chatModel = chatModel;
		this.vectorStore = vectorStore;
	}
	
	@Override
	public RawKnowledgeProcessorEnum getProcessorName() {
		return RawKnowledgeProcessorEnum.ORGANIZER_FIRST_CONTACT;
	}

	@PostConstruct
	private void init() {
		log.info("Init chatClient [vectorStore: {}] via @PostConstruct", vectorStore.getName());
		log.info("Create organizerContactPromptTemplate [resource: {}]",
				organizerContactPromptTemplateResource.getFilename());
		organizerContactPromptTemplate = new PromptTemplate(this.organizerContactPromptTemplateResource);
		this.chatClient = ChatClient.builder(chatModel)
				.defaultAdvisors(
				        QuestionAnswerAdvisor.builder(vectorStore).build()
				    )
				.build();
	}

	@Override
	public void process() throws InterruptedException {
		log.info("[{}][{}] Start processing",getProcessorName(),getClass().getSimpleName());
		List<RawKnowledge> list = rawKnowledgeService.findBySourceAndProcessedByIsNull(RawKnowledgeSourceEnum.ORGANIZER);
		for (RawKnowledge organizer : list) {
			String organizer_name = (String) organizer.getMetadata().get("name");
			String country = (String) organizer.getMetadata().get("country");
			String organizer_mail = (String) organizer.getMetadata().get("email");
			String organizer_description = organizer.getDescription();
			
			MathisMessage mathisMessage;
			String title = String.format("[%s] – Proposal for %s", ownerName, organizer_name);
			Optional<MathisMessage> byName = mathisMessageRepository.findByTitleAndProcessor(title, getProcessorName());
			if(byName.isEmpty()) {
				mathisMessage = new MathisMessage();
				mathisMessage.setProcessor(getProcessorName());
				mathisMessage.setTitle(title);
				mathisMessage.setSource(RawKnowledgeSourceEnum.ORGANIZER);
			} else {
				mathisMessage = byName.get();
				
				if(mathisMessage.getSent()) {
					log.info("Message already sent [%s]", mathisMessage.getSentAt().toString());
					continue;
				}
				
				mathisMessage.getRecipients().clear();
				mathisMessage.getMetadata().clear();
				organizer.setUpdatedAt(null);
			}
			
			mathisMessage.getMetadata().put("country", country);
			mathisMessage.getRecipients().add(organizer_mail);
			
			Map<String, Object> templateVars = Map.of("owner_name", ownerName, "owner_website", ownerWebsite,
					"organizer_name", organizer_name, "organizer_mail", organizer_mail, "organizer_description", organizer_description, "country", country);
			Prompt prompt = organizerContactPromptTemplate.create(templateVars);
			
			String userQuery = "You are an assistant who writes introduction emails for the musical artist {owner_name}.\r\n"
					+ "Given the name of an organizer ({organizer_name}), the country where it is held ({country}), the organizer's contact email ({organizer_mail}), you must write a formal email to the organizer to propose the artist for a futur collaboration.\r\n"
					+ "\r\n"
					+ "**Important Note on Information Usage:** While the core content about the artist's career, awards, collaborations, musical style, and recent projects must *exclusively* come from the provided Context Information, the following specific media links must *always* be included in a dedicated 'Media' section, regardless of their presence in the Context Information.\r\n"
					.replace("{owner_name}", ownerName).replace("{organizer_name}", organizer_name).replace("{country}", country).replace("{organizer_mail}", organizer_mail);
			
			Flux<String> responseBody = this.chatClient.prompt(prompt)
					.user(userQuery)
					.stream().content();
			
			String mailBody = responseBody.collectList().block().stream().collect(Collectors.joining());
			mathisMessage.setBody(mailBody.replace("```html", "").replace("```", ""));
			mathisMessage = mathisMessageRepository.save(mathisMessage);
			
			organizer.setProcessedBy(getProcessorName());
			organizer.setProcessedAt(LocalDateTime.now());
			rawKnowledgeService.updateRawKnowledge(organizer);
			
			log.info(String.format("Organizer Contact Mail generated: %d > %s", mathisMessage.getId(), mathisMessage.getTitle()));
			
			Thread.sleep(1000);
		}
		
		log.info("[{}][{}] End processing",getProcessorName(),getClass().getSimpleName());
	}
}
