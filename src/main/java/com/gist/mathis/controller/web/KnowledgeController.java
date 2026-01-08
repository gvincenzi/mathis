package com.gist.mathis.controller.web;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gist.mathis.controller.KnowledgeBaseController;
import com.gist.mathis.model.entity.Knowledge;
import com.gist.mathis.service.KnowledgeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/web/knowledge")
public class KnowledgeController {
	@Autowired
	private KnowledgeService knowledgeService;
	
	@GetMapping
    public String knowledges(Model model) {
		Set<Knowledge> knowledges = knowledgeService.findAll();
		model.addAttribute("knowledges",knowledges);
        return "knowledge";
    }
	
	@PostMapping("/update/{knowledgeId}")
    public String updateKnowledge(@PathVariable("knowledgeId") Long knowledgeId, @ModelAttribute Knowledge knowledge, RedirectAttributes redirectAttributes) {
		log.info(String.format("%s -> %s", KnowledgeController.class.getSimpleName(), "updateKnowledge"));
		knowledgeService.updateKnowledge(knowledge);
		
        redirectAttributes.addFlashAttribute("message", "Knowledge item updated successfully!");
        return "redirect:/web/knowledge";
    }
	
	@GetMapping("/delete/{knowledgeId}")
    public String deleteKnowledge(@PathVariable("knowledgeId") Long knowledgeId, RedirectAttributes redirectAttributes) {
		log.info(String.format("%s -> %s", KnowledgeController.class.getSimpleName(), "deleteKnowledge"));
		knowledgeService.deleteById(knowledgeId);
		
		redirectAttributes.addFlashAttribute("message", "Knowledge item deleted successfully!");
        return "redirect:/web/knowledge";
    }
	
	@GetMapping("/new")
    public String showCreateForm(Model model) {
        return "knowledge_create";
    }
	
	@PostMapping(value="/ingest", consumes = "multipart/form-data")
    public String ingest(@RequestParam("document") MultipartFile document, @RequestParam("title") String title, @RequestParam("description") String description, @RequestParam("url") String url, RedirectAttributes redirectAttributes) {
		log.info(String.format("%s -> %s", KnowledgeBaseController.class.getSimpleName(), "ingest"));
		
		Knowledge knowledge = new Knowledge();
		knowledge.setTitle(title);
		knowledge.setDescription(description);
		knowledge.setFilename(document.getOriginalFilename());
		knowledge.setUrl(url);
		
		knowledgeService.saveKnowledge(document.getResource(), knowledge);
		redirectAttributes.addFlashAttribute("message", "Knowledge item added successfully!");
        return "redirect:/web/knowledge";
    }
}
