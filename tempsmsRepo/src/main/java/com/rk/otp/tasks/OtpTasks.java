package com.rk.otp.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rk.otp.service.OtpService;

@Component
@EnableAsync
public class OtpTasks {

	@Autowired
	private OtpService otpService;

	@Scheduled(fixedDelay = 3000)
	@Async
	public void updateOtpStatus() {
		try {
			otpService.updateOtpStatus();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
