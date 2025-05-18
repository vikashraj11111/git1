package com.rk.otp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.rk.app.persistence.entity.User;
import com.rk.app.user.UserDetailsImpl;
import com.rk.otp.service.UserService;

@RestController
public class AppController {
	
	private static final String REDIRECT = "redirect:/home";
	
	@Autowired
	private UserService userService;

	@GetMapping(value = {"/"})
	public ModelAndView getHomePage(Authentication authentication) {
		return new ModelAndView(REDIRECT);
	}
	
	@GetMapping(value = {"/home"})
	public ModelAndView getHomePageNew(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if(authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			modelAndView.addObject("haveActiveNumber", userService.haveActiveNumbers(username));
		}
		modelAndView.setViewName("main");
		return modelAndView;
	}
	
	@GetMapping("/register")
	public ModelAndView showRegistrationForm(Authentication authentication) {
		if(authentication != null && !(authentication instanceof AnonymousAuthenticationToken))
			return new ModelAndView(REDIRECT);
		
		ModelAndView modelAndView = new ModelAndView();
		User user = new User();
		modelAndView.addObject("user", user);
		modelAndView.setViewName("registration");
		return modelAndView;
	}
	
	@GetMapping("/register_success")
	public ModelAndView showRegisterSuccessPage(Authentication authentication) {
		if(authentication != null && !(authentication instanceof AnonymousAuthenticationToken))
			return new ModelAndView(REDIRECT);
		
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("register_success");
		return modelAndView;
	}
}
