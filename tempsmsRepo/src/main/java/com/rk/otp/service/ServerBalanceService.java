package com.rk.otp.service;

public interface ServerBalanceService {

	String getServerBalance(String server);

	void saveServerBalance(String server, String balance);

}