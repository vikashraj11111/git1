package com.rk.otp.validation.utility;

public class Utility {
	
	private Utility() {
	}

	public static String extractCookie(String cookie) {
		return cookie.split(";")[0];
	}

	public static String extractCookieName(String cookie) {
		return cookie.split("=")[0];
	}

	public static String extractCookieValue(String cookie) {
		return cookie.split("=").length > 1 ? cookie.split("=")[1] : null;
	}

}
