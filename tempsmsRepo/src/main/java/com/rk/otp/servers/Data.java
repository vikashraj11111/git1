package com.rk.otp.servers;

import lombok.ToString;

@lombok.Data
@ToString
public class Data {

	private String apiKey;
	private String service;
	private String code;

}
