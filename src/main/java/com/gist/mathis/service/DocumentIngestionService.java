package com.gist.mathis.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class DocumentIngestionService{
	@Autowired
	private VectorStore vectorStore;
	
	public void ingest(Resource resource) {
		log.info(String.format("%s -> %s", DocumentIngestionService.class.getSimpleName(), "ingest"));
		log.info(String.format("resource -> %s", resource.getFilename()));

		log.info(String.format("%s -> %s", TikaDocumentReader.class.getSimpleName(), "read file"));
		TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
		
		log.info(String.format("%s -> %s", TokenTextSplitter.class.getSimpleName(), "split into chunks"));
		TextSplitter splitter = new TokenTextSplitter();
		List<Document> documents = splitter.split(tikaDocumentReader.read());
		
		log.info(String.format("%s -> %s", VectorStore.class.getSimpleName(), "store in vector database"));
		vectorStore.accept(documents);
	}
}
