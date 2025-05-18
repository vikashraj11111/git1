package com.rk.otp.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.ServerCostPerc;
import com.rk.app.persistence.repository.ServerCostPercRepo;
import com.rk.otp.service.ServerCostPercService;

@Service
public class ServerCostPercServiceImpl implements ServerCostPercService {

	@Autowired
	private ServerCostPercRepo serverCostPercRepo;
	
	@Override
	public double getPercentage(String server) {
		
		return serverCostPercRepo.getReferenceById(server).getPerc();
	}
	
	@Override
	public ServerCostPerc saveAndFlush(String server, double perc, double actualCost) {
		ServerCostPerc serverCostPerc = new ServerCostPerc(server, perc, actualCost);
		
		return serverCostPercRepo.saveAndFlush(serverCostPerc);
	}
	
	@Override
	public ServerCostPerc getById(String server) { 
		
		return serverCostPercRepo.getReferenceById(server);
	}

}
