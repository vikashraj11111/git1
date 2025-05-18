package com.rk.otp.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rk.app.persistence.entity.MailSentFlag;
import com.rk.app.persistence.repository.MailSentFlagRepo;
import com.rk.otp.service.MailSentFlagService;

@Service
public class MailSentFlagServiceImpl implements MailSentFlagService {

	@Autowired
	private MailSentFlagRepo mailSentFlagRepo;
	
	@Override
	public void save(String server, boolean flag, String error) {
		
		MailSentFlag mailSentFlag = new MailSentFlag();
		mailSentFlag.setServer(server);
		mailSentFlag.setMailSent(flag);
		mailSentFlag.setError(error);
		
		mailSentFlagRepo.saveAndFlush(mailSentFlag);
	}
	
	@Override
	public int getByServerAndError(String server, String error) {
		MailSentFlag mailSentFlag = mailSentFlagRepo.findByServerAndError(server, error).orElse(null);
		if (mailSentFlag == null)
			return -1;
		long seconds = TimeUnit.MILLISECONDS
				.toSeconds(Timestamp.valueOf(LocalDateTime.now()).getTime() - mailSentFlag.getInsertTime().getTime());
		if (seconds > 120) {
			mailSentFlagRepo.delete(mailSentFlag);
			return -1;
		}
		return 1;
	}
	
	@Override
	public boolean isMailSentForServer(String server) {
		
		List<MailSentFlag> mailList = mailSentFlagRepo.findAllByServer(server);
		if(mailList.isEmpty())
			return true;
		
		return false;
	}
	
	@Override
	@Transactional
	public void deleteAllByServer(String server) {
		
		mailSentFlagRepo.deleteByServer(server);
	}
	
	
}
