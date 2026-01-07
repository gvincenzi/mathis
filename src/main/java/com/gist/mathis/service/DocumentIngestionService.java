package com.gist.mathis.service;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.Knowledge;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentIngestionService{
	@Autowired
	private VectorStore vectorStore;
	
	public void ingest(Resource resource, Knowledge knowledge) {
        log.info("{} -> ingest", DocumentIngestionService.class.getSimpleName());
        log.info("resource filename: {}", resource.getFilename());

        log.info("{} -> read file", TikaDocumentReader.class.getSimpleName());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        log.info("{} -> split into chunks", TokenTextSplitter.class.getSimpleName());
        TextSplitter splitter = new TokenTextSplitter();
        List<Document> documents = splitter.split(tikaDocumentReader.read());

        documents.forEach(doc -> 
        {
        	Map<String, Object> metadata = doc.getMetadata();
            metadata.put("knowledge_id", knowledge.getKnowledgeId());
            metadata.put("filename", knowledge.getFilename());
            metadata.put("description", knowledge.getDescription());
            metadata.put("url", knowledge.getUrl());
        });

        log.info("{} -> store in vector database ({} documents)", VectorStore.class.getSimpleName(), documents.size());
        vectorStore.accept(documents);
    }
	
	public void updateMetadata(Knowledge knowledge) {
		log.info("{} -> updateMetadata", DocumentIngestionService.class.getSimpleName());
		
		SearchRequest request = SearchRequest.builder()
			    .filterExpression(String.format("knowledge_id == %d",knowledge.getKnowledgeId()))
			    .similarityThresholdAll()
			    .topK(1000)
			    .build();
		List<Document> documents = vectorStore.similaritySearch(request);
		
		documents.forEach(doc -> 
        {
        	Map<String, Object> metadata = doc.getMetadata();
            metadata.put("knowledge_id", knowledge.getKnowledgeId());
            metadata.put("filename", knowledge.getFilename());
            metadata.put("description", knowledge.getDescription());
            metadata.put("url", knowledge.getUrl());
        });
		
		log.info("{} -> update in vector database ({} documents)", VectorStore.class.getSimpleName(), documents.size());
        vectorStore.accept(documents);
	}
}
