package com.gist.mathis.controller.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gist.mathis.model.entity.RawKnowledge;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.service.RawKnowledgeService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/web/rawknowledge")
public class RawKnowledgeController {
    @Autowired
    private RawKnowledgeService rawKnowledgeService;
    
    private final static ObjectMapper objectMapper = new ObjectMapper();


    @GetMapping
    public String rawKnowledges(Model model) {
        List<RawKnowledge> rawKnowledges = rawKnowledgeService.findAll();
        Map<Long, String> metadataJsonMap = new HashMap<>();
        for (RawKnowledge rk : rawKnowledges) {
            try {
                metadataJsonMap.put(rk.getId(), objectMapper.writeValueAsString(rk.getMetadata()));
            } catch (Exception e) {
                metadataJsonMap.put(rk.getId(), "{}");
            }
        }
        
        model.addAttribute("rawKnowledges", rawKnowledges);
        model.addAttribute("metadataJsonMap", metadataJsonMap);
        model.addAttribute("rawKnowledgeSources", RawKnowledgeSourceEnum.values());
        return "rawknowledge";
    }
    
    @PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/update/{knowledgeId}")
    public String updateRawKnowledge(@PathVariable("knowledgeId") Long rawKnowledgeId, @ModelAttribute RawKnowledge rawKnowledge, RedirectAttributes redirectAttributes) {
		log.info(String.format("%s -> %s", RawKnowledgeController.class.getSimpleName(), "updateRawKnowledge"));
		rawKnowledgeService.updateRawKnowledge(rawKnowledge);
		
		redirectAttributes.addFlashAttribute("message", "RawKnowledge updated successfully!");
		return "redirect:/web/rawknowledge";
    }
    
    @PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/delete/{rawKnowledgeId}")
    public String deleteRawKnowledge(@PathVariable("rawKnowledgeId") Long rawKnowledgeId, RedirectAttributes redirectAttributes) {
		log.info(String.format("%s -> %s", KnowledgeController.class.getSimpleName(), "deleteKnowledge"));
		rawKnowledgeService.deleteById(rawKnowledgeId);
		
		redirectAttributes.addFlashAttribute("message", "Knowledge item deleted successfully!");
        return "redirect:/web/rawknowledge";
    }
}
