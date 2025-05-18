package com.rk.otp.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rk.otp.service.OtpService;

@Component
public class CodeNumberTasks {

	@Autowired
	private OtpService otpService;

	@Scheduled(fixedDelay = 1000)
	public void refundUserIfOtpNotReceivedIn20Mins() { // after 20 min
		synchronized (this) {
			try {
				otpService.refundUserIfOtpNotReceivedIn20Mins();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Scheduled(fixedDelay = 1000)
	public void deleteCodeNumberAfter25Mins() { // after 25 min
		synchronized (this) {
			try {
				otpService.deleteCodeNumberAfter25Mins();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

//	@Scheduled(fixedDelay = 5000)
	public void deleteUsedCancelled() {
		synchronized (this) {
			try {
				otpService.deleteUsedCancelled();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
