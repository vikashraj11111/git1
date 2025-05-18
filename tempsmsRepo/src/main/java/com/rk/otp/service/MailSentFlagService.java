package com.rk.otp.service;

public interface MailSentFlagService {

	void save(String server, boolean flag, String error);

	int getByServerAndError(String server, String error);

	boolean isMailSentForServer(String server);

	void deleteAllByServer(String server);

}