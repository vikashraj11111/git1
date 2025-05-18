package com.rk.otp.validation.service;

import org.springframework.http.ResponseEntity;

public interface CheckRegistrationStatusService {

	ResponseEntity<?> checkFlipkartStatus(String number);

	ResponseEntity<?> checkDream11Status(String number);

	ResponseEntity<?> checkSwiggyStatus(String number);

	ResponseEntity<?> checkPaytmStatus(String number);

	ResponseEntity<?> checkMyntraStatus(String number);

}
