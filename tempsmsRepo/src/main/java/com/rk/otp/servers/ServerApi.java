package com.rk.otp.servers;

public interface ServerApi {
	
	String getBalance(String apiKey);

	String getPrice(String apiKey, String service);

	String getNumber(String apiKey, String service);

	String getOtp(String apiKey, String code);
	
	String getFullSms(String apiKey, String code);

	String cancel(String apiKey, String code);

	String getPriceAll(String apiKey);

	String getCount(String apiKey, String service);

	String resend(String apiKey, String code);

	String getNumberByCountry(String apiKey, String service, String country);

}