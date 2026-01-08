package com.gist.mathis.controller.web;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gist.mathis.model.entity.MathisUser;
import com.gist.mathis.service.security.MathisUserDetailsService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/web/users")
public class MathisUserController {

	@Autowired
	private MathisUserDetailsService mathisUserService;

	@GetMapping
	public String users(Model model) {
		List<MathisUser> users = mathisUserService.findAll();
		model.addAttribute("users", users);
		return "users";
	}

	@GetMapping("/new")
	public String showCreateForm(Model model) {
		model.addAttribute("user", new MathisUser());
		return "user_create";
	}

	@PostMapping("/create")
	public String createUser(@RequestParam String username, @RequestParam String password,
			@RequestParam(required = false) String firstname, @RequestParam(required = false) String lastname,
			@RequestParam("auth") String auth, RedirectAttributes redirectAttributes) {
		MathisUser user = new MathisUser();
		user.setUsername(username);
		user.setPassword(password);
		user.setFirstname(firstname);
		user.setLastname(lastname);
		user.setAuth(com.gist.mathis.model.entity.AuthorityEnum.valueOf(auth));
		mathisUserService.saveUser(user);
		redirectAttributes.addFlashAttribute("message", "User created successfully!");
		return "redirect:/web/users";
	}

	@PostMapping("/update/{id}")
	public String updateUser(@PathVariable("id") Long userId, @ModelAttribute MathisUser user,
			RedirectAttributes redirectAttributes) {
		MathisUser existing = mathisUserService.findById(userId).orElseThrow(() -> new NoSuchElementException(String.format("User with ID %d not found",userId)));
		existing.setUsername(user.getUsername());
		existing.setFirstname(user.getFirstname());
		existing.setLastname(user.getLastname());
		existing.setAuth(user.getAuth());
		mathisUserService.updateUser(existing);
		redirectAttributes.addFlashAttribute("message", "User updated successfully!");
		return "redirect:/web/users";
	}

	@GetMapping("/delete/{id}")
	public String deleteUser(@PathVariable("id") Long userId, RedirectAttributes redirectAttributes) {
		mathisUserService.deleteUser(userId);
		redirectAttributes.addFlashAttribute("message", "User deleted successfully!");
		return "redirect:/web/users";
	}
}
