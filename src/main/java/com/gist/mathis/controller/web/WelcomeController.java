package com.gist.mathis.controller.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WelcomeController {
	@Value("${server.servlet.context-path:/}")
    String contextPath;
	
	@GetMapping("/web/homepage")
    public String homepage(Model model) {
		model.addAttribute("contextPath", contextPath);
        return "homepage";
    }
	
	@GetMapping("/web/fullchat")
    public String fullChatPage(Model model) {
        model.addAttribute("showFullChatButton", false);
        model.addAttribute("contextPath", contextPath);
        return "full_chat";
    }
}
