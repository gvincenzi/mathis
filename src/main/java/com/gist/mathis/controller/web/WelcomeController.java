package com.gist.mathis.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class WelcomeController {
	@GetMapping("/web/homepage")
    public String cashflow(Model model) {
        return "homepage";
    }
}
