package com.rk.otp.service;

import org.springframework.security.core.Authentication;

import com.rk.app.persistence.entity.CodeNumber;

public interface OtpService {

	String getCodeNumber(Double amount, String server, String service, String username, int retry);

	String getOtpByCode(String service, String server, String code, String username, CodeNumber codeNumber, boolean isAdmin);

	boolean cancelActivation(String code, Authentication authentication);

	String getPriceAll(String server);

	String getBalance(String server);

	boolean resend(String code, Authentication authentication);

	String verifyKey(String updatedKey, String server);

	Double getPriceFromServer(String service, String server);

	void refundUserIfOtpNotReceivedIn20Mins();

	void deleteCodeNumberAfter25Mins();

	void deleteUsedCancelled();

	void updateOtpStatus();

	void cancelNumbers(String username);

}
