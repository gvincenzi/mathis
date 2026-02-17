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
import com.gist.mathis.model.entity.MathisMessage;
import com.gist.mathis.model.entity.RawKnowledgeProcessorEnum;
import com.gist.mathis.model.entity.RawKnowledgeSourceEnum;
import com.gist.mathis.service.MathisMessageService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/web/mathismessage")
public class MathisMessageController {
    @Autowired
    private MathisMessageService mathisMessageService;
    
    private final static ObjectMapper objectMapper = new ObjectMapper();


    @GetMapping
    public String mathisMessages(Model model) {
        List<MathisMessage> mathisMessages = mathisMessageService.findAll();
        Map<Long, String> metadataJsonMap = new HashMap<>();
        for (MathisMessage mm : mathisMessages) {
            try {
                metadataJsonMap.put(mm.getId(), objectMapper.writeValueAsString(mm.getMetadata()));
            } catch (Exception e) {
                metadataJsonMap.put(mm.getId(), "{}");
            }
        }
        model.addAttribute("mathisMessages", mathisMessages);
        model.addAttribute("metadataJsonMap", metadataJsonMap);
        model.addAttribute("rawKnowledgeSources", RawKnowledgeSourceEnum.values());
        model.addAttribute("rawKnowledgeProcessors", RawKnowledgeProcessorEnum.values());
        return "mathismessage";
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/update/{id}")
    public String updateMathisMessage(@PathVariable("id") Long id, @ModelAttribute MathisMessage mathisMessage,
                                      RedirectAttributes redirectAttributes) {
        mathisMessageService.updateMathisMessage(mathisMessage);
        redirectAttributes.addFlashAttribute("message", "MathisMessage updated successfully!");
        return "redirect:/web/mathismessage";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteMathisMessage(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        mathisMessageService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "MathisMessage deleted successfully!");
        return "redirect:/web/mathismessage";
    }
}
