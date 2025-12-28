package com.gist.mathis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.gist.mathis.model.entity.Note;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmbeddingService{
	@Autowired
	private VectorStore vectorStore;
	
	public void ingest(Resource resource) {
        log.info("{} -> ingest", EmbeddingService.class.getSimpleName());
        log.info("resource filename: {}", resource.getFilename());

        log.info("{} -> read file", TikaDocumentReader.class.getSimpleName());
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);

        log.info("{} -> split into chunks", TokenTextSplitter.class.getSimpleName());
        TextSplitter splitter = new TokenTextSplitter();
        List<Document> documents = splitter.split(tikaDocumentReader.read());

        log.info("{} -> store in vector database ({} documents)", VectorStore.class.getSimpleName(), documents.size());
        vectorStore.accept(documents);
    }
	
	public void noteEmbeddings(Note note) {
        List<String> chunks = splitText(note.getContent(), 512);

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("note_id", note.getNoteId());
            metadata.put("chunk_index", i);
            metadata.put("title", note.getTitle());
            metadata.put("notebook_id", note.getNotebook().getNotebookId());
            metadata.put("notebook_title", note.getNotebook().getTitle());

            Document document = new Document(UUID.randomUUID().toString(), chunks.get(i), metadata);
            documents.add(document);
        }
        vectorStore.accept(documents);
    }
	
	public static List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(len, i + chunkSize)));
        }
        return chunks;
    }
}
