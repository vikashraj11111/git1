package com.rk.otp.controller;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.rk.app.exception.InvalidPasswordException;
import com.rk.app.exception.InvalidUsernameException;
import com.rk.app.exception.UserAlreadyExistsException;
import com.rk.app.persistence.entity.User;
import com.rk.otp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/registration")
public class RegistrationController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);

	private final UserService userService;

	public RegistrationController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	public ResponseEntity<?> registerUserAccount(@RequestBody @Valid User user, HttpServletRequest request) {
		try {
			return new ResponseEntity<>(new Gson().toJson(userService.register(user, request)), HttpStatus.OK);
		} catch (UserAlreadyExistsException e) {
			LOGGER.error("Exception at registerUserAccount");

			return new ResponseEntity<>(new Gson().toJson("Username already exists"), HttpStatus.OK);
//		} catch (EmailAlreadyExistsException e) {
//			LOGGER.error("Exception at registerUserAccount");
//			
//			return new ResponseEntity<>(new Gson().toJson("Email already exists"), HttpStatus.OK);
		} catch (ConstraintViolationException cvex) {
			return new ResponseEntity<>(new Gson().toJson(cvex.getConstraintViolations().stream()
					.map(ConstraintViolation::getPropertyPath).map(Path::toString).collect(Collectors.toList())),
					HttpStatus.OK);
		} catch (InvalidUsernameException ex) {
			LOGGER.error("Exception at registerUserAccount :: Invalid username");

			return new ResponseEntity<>(new Gson().toJson("Invalid Username"), HttpStatus.OK);
		} catch (InvalidPasswordException ex) {
			LOGGER.error("Exception at registerUserAccount :: Invalid password");

			return new ResponseEntity<>(new Gson().toJson("Invalid Password"), HttpStatus.OK);
		}
	}

}
