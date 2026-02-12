package com.gist.mathis.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.repository.RawKnowledgeRepository;

@Service
public class RawKnowledgeService {
	@Autowired
	private RawKnowledgeRepository rawKnowledgeRepository;
	
	public List<RawKnowledge> findAll() {
		return rawKnowledgeRepository.findAll();
	}

	public RawKnowledge updateRawKnowledge(RawKnowledge rawKnowledge) {
		rawKnowledge.setUpdatedAt(null);
		rawKnowledge = rawKnowledgeRepository.save(rawKnowledge);
        return rawKnowledge;
	}

	public void deleteById(Long rawKnowledgeId) {
		rawKnowledgeRepository.deleteById(rawKnowledgeId);
	}
}
