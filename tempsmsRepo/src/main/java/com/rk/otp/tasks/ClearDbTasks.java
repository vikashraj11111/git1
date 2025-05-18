package com.rk.otp.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rk.otp.service.UserBalanceAuditService;
import com.rk.otp.service.UsersOtpHistoryService;

@Component
@EnableAsync
public class ClearDbTasks {

	@Autowired
	private UserBalanceAuditService userBalanceAuditService;

	@Autowired
	private UsersOtpHistoryService usersOtpHistoryService;

	@Async
	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
	public void clearDb() {
		System.out.println("Starting clearDb Task :::");

		try {
			userBalanceAuditService.clearUserBalanceAudit();

			usersOtpHistoryService.clearOtpHistory();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("clearDb Task ends ::: ");
	}

}
