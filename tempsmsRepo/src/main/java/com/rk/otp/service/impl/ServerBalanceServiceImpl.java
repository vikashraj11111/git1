package com.rk.otp.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.ServerBalance;
import com.rk.app.persistence.repository.ServerBalanceRepo;
import com.rk.otp.service.ServerBalanceService;

@Service
public class ServerBalanceServiceImpl implements ServerBalanceService {

	@Autowired
	ServerBalanceRepo serverBalanceRepo;
	
	@Override
	public String getServerBalance(String server) {
		
		return serverBalanceRepo.getReferenceById(server).getBalance();
	}
	
	@Override
	public void saveServerBalance(String server, String balance) {
		ServerBalance serverBalance = new ServerBalance();
		serverBalance.setServer(server);
		serverBalance.setBalance(balance);
		
		serverBalanceRepo.saveAndFlush(serverBalance);
	}
}
