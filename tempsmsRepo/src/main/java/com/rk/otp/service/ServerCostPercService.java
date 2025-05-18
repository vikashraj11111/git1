package com.rk.otp.service;

import com.rk.app.persistence.entity.ServerCostPerc;

public interface ServerCostPercService {

	double getPercentage(String server);

	ServerCostPerc saveAndFlush(String server, double perc, double actualCost);

	ServerCostPerc getById(String server);

}