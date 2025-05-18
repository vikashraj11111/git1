package com.rk.otp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rk.otp.validation.service.CheckRegistrationStatusService;

@RestController
@RequestMapping("/verify")
public class CheckRegistrationStatusController {
	
	@Autowired
	private CheckRegistrationStatusService registrationStatusService;
	
	@GetMapping("/flipkart/{number}")
	public ResponseEntity<?> checkFlipkartStatus(@PathVariable("number") String number, Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken))
			return registrationStatusService.checkFlipkartStatus(number);
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not authorized MotherF&#ker");
	}
	
	@GetMapping("/dream11/{number}")
	public ResponseEntity<?> checkDream11Status(@PathVariable("number") String number, Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken))
			return registrationStatusService.checkDream11Status(number);
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not authorized MotherF&#ker");
	}
	
	@GetMapping("/swiggy/{number}")
	public ResponseEntity<?> checkSwiggyStatus(@PathVariable("number") String number, Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken))
			return registrationStatusService.checkSwiggyStatus(number);
		
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not authorized MotherF&#ker");
	}
	
}
