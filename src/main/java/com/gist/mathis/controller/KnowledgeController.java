package com.gist.mathis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.service.KnowledgeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
	@Autowired
	private KnowledgeService knowledgeService;
	
	@PostMapping(value ="/ingest", consumes = "multipart/form-data")
    public ResponseEntity<Void> ingest(@RequestParam("document") MultipartFile document, @RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("url") String url) {
		log.info(String.format("%s -> %s", KnowledgeController.class.getSimpleName(), "ingest"));
		
		Knowledge knowledge = new Knowledge();
		knowledge.setTitle(title);
		knowledge.setDescription(description);
		knowledge.setFilename(document.getOriginalFilename());
		knowledge.setUrl(url);
		
		knowledgeService.saveKnowledge(document.getResource(), knowledge);
        return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
    }
}
