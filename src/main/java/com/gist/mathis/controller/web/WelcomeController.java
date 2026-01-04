package com.gist.mathis.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WelcomeController {
	@GetMapping("/web/homepage")
    public String homepage(Model model) {
        return "homepage";
    }
	
	@GetMapping("/web/fullchat")
    public String fullChatPage(Model model) {
        model.addAttribute("showFullChatButton", false);
        return "full_chat";
    }
}
