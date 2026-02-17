package com.gist.mathis.service.processor;

import com.gist.mathis.model.entity.RawKnowledgeProcessorEnum;

public interface KnowledgeProcessor {
	RawKnowledgeProcessorEnum getProcessorName();
	void process() throws InterruptedException;
}
