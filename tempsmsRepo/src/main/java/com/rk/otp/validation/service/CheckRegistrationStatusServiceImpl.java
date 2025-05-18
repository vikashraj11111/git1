package com.rk.otp.validation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CheckRegistrationStatusServiceImpl implements CheckRegistrationStatusService {

	@Autowired
	private RegistrationStatusService registrationStatusService;

	@Override
	public ResponseEntity<?> checkFlipkartStatus(String number) {
		if (number.length() == 10) {
			return ResponseEntity.ok().body(registrationStatusService.checkFlipkartRegistrationStatus(number));
		}
		return ResponseEntity.ok("INVALID");
	}

	@Override
	public ResponseEntity<?> checkDream11Status(String number) {
		if (number.length() == 10) {
			return ResponseEntity.ok().body(registrationStatusService.checkDream11RegistrationStatus(number));
		}
		return ResponseEntity.ok("INVALID");
	}

	@Override
	public ResponseEntity<?> checkSwiggyStatus(String number) {
		try {
			if (number.length() == 10) {
				boolean response = registrationStatusService.checkSwiggyRegistrationStatus(number, 0);
				return ResponseEntity.ok().body(response);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("Failed");
		}
		return ResponseEntity.ok("INVALID");
	}

	@Override
	public ResponseEntity<?> checkPaytmStatus(String number) {
		if (number.length() == 10) {
			return ResponseEntity.ok().body(registrationStatusService.checkPaytmRegistrationStatus(number));
		}
		return ResponseEntity.ok("INVALID");
	}

	@Override
	public ResponseEntity<?> checkMyntraStatus(String number) {
		if (number.length() == 10) {
			return ResponseEntity.ok().body(registrationStatusService.checkMyntraRegistrationStatus(number));
		}
		return ResponseEntity.ok("INVALID");
	}

}
