package com.gist.mathis.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.entity.RawKnowledgeProcessorEnum;

public interface MathisMessageRepository extends JpaRepository<MathisMessage, Long> {
	Optional<MathisMessage> findByTitleAndProcessor(String title, RawKnowledgeProcessorEnum processorName);
}
