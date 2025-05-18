package com.rk.otp.tasks;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rk.otp.db.backup.service.BackupService;

@Component
@EnableAsync
public class BackupTasks {

	@Autowired
	private BackupService backupService;

	@Async
	@Scheduled(cron = "0 0 * * * *")
	public void backupUsersTask() throws IOException {
		System.out.println("Starting backup users Task :::");

		try {
			backupService.backupUserDetails();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Backup users Task ends :::");
	}

	@Async
	@Scheduled(cron = "0 0 */8 * * *", zone = "Asia/Kolkata")
	public void backupEverythingTask() {
		System.out.println("Starting backup everything Task :::");

		try {
			backupService.backupEverything();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Backup everything Task ends :::");
	}

}
