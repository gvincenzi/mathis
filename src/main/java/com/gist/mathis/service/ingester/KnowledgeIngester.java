package com.gist.mathis.service.ingester;

import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;

public interface KnowledgeIngester {
	RawKnowledgeSourceEnum getSourceName();
	void ingest();
}
