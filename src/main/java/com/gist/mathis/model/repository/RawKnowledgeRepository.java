package com.gist.mathis.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;

public interface RawKnowledgeRepository extends JpaRepository<RawKnowledge, Long> {
	Optional<RawKnowledge> findByNameAndSource(String name, RawKnowledgeSourceEnum source);
}
