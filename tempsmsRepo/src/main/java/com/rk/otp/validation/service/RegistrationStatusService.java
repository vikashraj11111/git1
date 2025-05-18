package com.rk.otp.validation.service;

public interface RegistrationStatusService {

	boolean checkFlipkartRegistrationStatus(String number);

	boolean checkDream11RegistrationStatus(String number);

	boolean checkSwiggyRegistrationStatus(String number, int retryCount);

	boolean checkMyntraRegistrationStatus(String number);

	boolean checkPaytmRegistrationStatus(String number);

	String checkLazyPaySimplEligibilityMakeMyTrip(String number, int retry, String bankName);

	String checkLazyPaySimplEligibilityPayU(String number, int retry, String bankName, String paymentId, String accessToken);
	
	String checkLazyPaySimplEligibilityRazor(String number, int retry, String keyId, boolean isSimpl, boolean isLazyPay);

	int checkLazyPaySimplEligibilityAmount(String number, String keyId, boolean isSimpl, boolean isLazyPay);

	String getMmtCheckoutPage(String number);

	String checkLazyPayEligibilityCashFree(String number);

}
