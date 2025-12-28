package com.gist.mathis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gist.mathis.service.EmbeddingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
	@Autowired
	private EmbeddingService documentIngestionService;
	
	@PostMapping(value ="/upload", consumes = "multipart/form-data")
    public ResponseEntity<Void> ingest(@RequestParam("document") MultipartFile document) {
		log.info(String.format("%s -> %s", KnowledgeController.class.getSimpleName(), "ingest"));
		documentIngestionService.ingest(document.getResource());
        return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
    }
}
