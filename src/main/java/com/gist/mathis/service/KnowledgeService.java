package com.gist.mathis.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.model.repository.KnowledgeRepository;

@Service
public class KnowledgeService {
    @Autowired
    private KnowledgeRepository knowledgeRepository;
    
    @Autowired
	private DocumentIngestionService documentIngestionService;

    public Knowledge saveKnowledge(Resource resource, Knowledge knowledge) {
        knowledge = knowledgeRepository.save(knowledge);
        documentIngestionService.ingest(resource, knowledge);
        return knowledge;
    }
    
    public List<Knowledge> findAll(){
    	return knowledgeRepository.findAll();
    }
    
    public Optional<Knowledge> findById(Long knowledgeId){
    	return knowledgeRepository.findById(knowledgeId);
    }
}
