package com.rk.otp.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class AuthenticationController {
	
	private static final String REDIRECT = "redirect:/";
	private static final String LOGIN = "login";
	
	@RequestMapping(value = "/login")
	public ModelAndView login(HttpServletRequest req, Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("session", req.getSession());
		if(isAuthenticated(authentication))
			modelAndView.setViewName(REDIRECT);
		else
			modelAndView.setViewName(LOGIN);
		return modelAndView;
	}
	
	@GetMapping(value = "/loginRedirect")
	public ModelAndView loginRedirect(HttpServletRequest req, Authentication authentication) {
		req.getSession().setAttribute("loginRedirect", true);
		return new ModelAndView(LOGIN);
	}
	
	private boolean isAuthenticated(Authentication authentication) {
		return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
	}
	
}
