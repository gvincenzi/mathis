package com.gist.mathis.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.Filter.ExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.model.repository.KnowledgeRepository;
import com.gist.mathis.service.entity.IntentResponse;

@Service
public class KnowledgeService {
    @Autowired
    private KnowledgeRepository knowledgeRepository;
    
    @Autowired
	private DocumentIngestionService documentIngestionService;
    
    @Autowired
    private VectorStore vectorStore;
    
    private SearchRequest searchRequest = SearchRequest.builder().build();

    public Knowledge saveKnowledge(Resource resource, Knowledge knowledge) {
        knowledge = knowledgeRepository.save(knowledge);
        documentIngestionService.ingest(resource, knowledge);
        return knowledge;
    }
    
    public Knowledge updateKnowledge(Knowledge knowledge) {
    	knowledge.setUpdatedAt(null);
        knowledge = knowledgeRepository.save(knowledge);
        return knowledge;
    }
    
    public List<Knowledge> findAll(){
    	return knowledgeRepository.findAllByOrderByCreatedAtAsc();
    }
    
    public Optional<Knowledge> findById(Long knowledgeId){
    	return knowledgeRepository.findById(knowledgeId);
    }
    
    public Set<Knowledge> findByVectorialSearch(IntentResponse intent){
    	var searchRequestToUse = SearchRequest.from(this.searchRequest)
    		.query(intent.getEntities().get("document_title"))
    		.similarityThreshold(0.7D)
    		.build();
    	List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
    	
    	Set<Knowledge> knowledges = new HashSet<Knowledge>();
		documents.stream().forEach(doc -> {
			Optional<Knowledge> knowledge = findById(Long.valueOf((Integer)doc.getMetadata().get("knowledge_id")));
			if(knowledge.isPresent()) knowledges.add(knowledge.get());
		});
		
		return knowledges;
    }

	public void deleteById(Long knowledgeId) {
		knowledgeRepository.deleteById(knowledgeId);
		var exp = new Filter.Expression(ExpressionType.EQ, new Filter.Key("knowledge_id"), new Filter.Value(knowledgeId));
		vectorStore.delete(exp);
	}
	
}
