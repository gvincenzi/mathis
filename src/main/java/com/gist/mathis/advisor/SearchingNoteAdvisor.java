package com.gist.mathis.advisor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.gist.mathis.service.entity.IntentResponse;

import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class SearchingNoteAdvisor implements BaseAdvisor {
	public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

	public static final String FILTER_EXPRESSION = "qa_filter_expression";
	
	private static final int DEFAULT_ORDER = 0;
	
	public static final String INTENT_REPONSE = "intent_response";

	private final VectorStore vectorStore;

	private final PromptTemplate promptTemplate;

	private final SearchRequest searchRequest;

	@SuppressWarnings("unused")
	private final Scheduler scheduler;

	@SuppressWarnings("unused")
	private final int order;
	
	private static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = new PromptTemplate("""
			{query}

			Context information is below, surrounded by ---------------------

			---------------------
			{question_answer_context}
			---------------------

			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.
			""");

	SearchingNoteAdvisor(VectorStore vectorStore, SearchRequest searchRequest, @Nullable PromptTemplate promptTemplate,
			@Nullable Scheduler scheduler, int order) {
		Assert.notNull(vectorStore, "vectorStore cannot be null");
		Assert.notNull(searchRequest, "searchRequest cannot be null");

		this.vectorStore = vectorStore;
		this.searchRequest = searchRequest;
		this.promptTemplate = promptTemplate != null ? promptTemplate : DEFAULT_PROMPT_TEMPLATE;
		this.scheduler = scheduler != null ? scheduler : BaseAdvisor.DEFAULT_SCHEDULER;
		this.order = order;
	}
	
	public static Builder builder(VectorStore vectorStore) {
		return new Builder(vectorStore);
	}

	@Override
	public int getOrder() {
		return DEFAULT_ORDER;
	}

	@Override
	public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
		IntentResponse intentResponse = getIntentResponse(chatClientRequest.context());
		var exp = new Filter.Expression(ExpressionType.EQ, new Filter.Key("notebook_title"), new Filter.Value(intentResponse.getEntities().get("notebook_title")));
		// 1. Search for similar documents in the vector store.
		var searchRequestToUse = SearchRequest.from(this.searchRequest)
			.query(intentResponse.getEntities().get("content_to_search"))
			.similarityThreshold(0.7D)
			.filterExpression(exp)
			.build();

		List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);

		// 2. Create the context from the documents.
		Map<String, Object> context = new HashMap<>(chatClientRequest.context());
		context.put(RETRIEVED_DOCUMENTS, documents);

		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);

		String documentContext = documents.stream()
			    .map(doc -> {
			            try {
							return mapper.writeValueAsString(doc.getMetadata());
						} catch (JsonProcessingException e) {
							return "";
						}
			    })
			    .collect(Collectors.joining(System.lineSeparator()));

		// 3. Augment the user prompt with the document context.
		UserMessage userMessage = chatClientRequest.prompt().getUserMessage();
		String augmentedUserText = this.promptTemplate
			.render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));

		// 4. Update ChatClientRequest with augmented prompt.
		return chatClientRequest.mutate()
			.prompt(chatClientRequest.prompt().augmentUserMessage(augmentedUserText))
			.context(context)
			.build();
	}

	@Override
	public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
		ChatResponse.Builder chatResponseBuilder;
		if (chatClientResponse.chatResponse() == null) {
			chatResponseBuilder = ChatResponse.builder();
		}
		else {
			chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
		}
		chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
		return ChatClientResponse.builder()
			.chatResponse(chatResponseBuilder.build())
			.context(chatClientResponse.context())
			.build();
	}
	
	@Nullable
	protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {
		if (!context.containsKey(FILTER_EXPRESSION)
				|| !StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
			return this.searchRequest.getFilterExpression();
		}
		return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());
	}
	
	/**
	 * Retrieve the conversation ID from the given context or return the default
	 * conversation ID when not found.
	 */
	private IntentResponse getIntentResponse(Map<String, Object> context) {
		Assert.notNull(context, "context cannot be null");
		Assert.noNullElements(context.keySet().toArray(), "context cannot contain null keys");
		return context.containsKey(INTENT_REPONSE) ? (IntentResponse) context.get(INTENT_REPONSE) : null;
	}

	public static final class Builder {

		private final VectorStore vectorStore;

		private SearchRequest searchRequest = SearchRequest.builder().build();

		private PromptTemplate promptTemplate;

		private Scheduler scheduler;

		private int order = DEFAULT_ORDER;

		private Builder(VectorStore vectorStore) {
			Assert.notNull(vectorStore, "The vectorStore must not be null!");
			this.vectorStore = vectorStore;
		}

		public Builder promptTemplate(PromptTemplate promptTemplate) {
			Assert.notNull(promptTemplate, "promptTemplate cannot be null");
			this.promptTemplate = promptTemplate;
			return this;
		}

		public Builder searchRequest(SearchRequest searchRequest) {
			Assert.notNull(searchRequest, "The searchRequest must not be null!");
			this.searchRequest = searchRequest;
			return this;
		}

		public Builder protectFromBlocking(boolean protectFromBlocking) {
			this.scheduler = protectFromBlocking ? BaseAdvisor.DEFAULT_SCHEDULER : Schedulers.immediate();
			return this;
		}

		public Builder scheduler(Scheduler scheduler) {
			this.scheduler = scheduler;
			return this;
		}

		public Builder order(int order) {
			this.order = order;
			return this;
		}

		public SearchingNoteAdvisor build() {
			return new SearchingNoteAdvisor(this.vectorStore, this.searchRequest, this.promptTemplate, this.scheduler,
					this.order);
		}

	}
}
