package com.rk.otp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rk.otp.validation.service.CheckRegistrationStatusService;

@RestController
@RequestMapping("/okay/v1")
public class APIController {
	
	@Autowired
	private CheckRegistrationStatusService checkRegistrationStatusService;
	
	@GetMapping("/swiggy")
	public ResponseEntity<?> getSwiggy() {
		return checkRegistrationStatusService.checkSwiggyStatus("7000623977");
	}

}
