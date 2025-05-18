package com.rk.otp.utility;

import com.rk.app.utility.Utility;
import com.rk.otp.recharge.entity.OrderDetails;

public class AppUtility {

	private AppUtility() {
	}

	public static OrderDetails createOrder(String amount, String currency) {
		String username = Utility.getAuthenticatedUserName();
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setAmount(Double.parseDouble(amount));
		orderDetails.setCurrency(currency);
		String description = "Recharge Request of amount Rs. " + amount + " for user - " + username;
		if ("TRX".equalsIgnoreCase(currency)) {
			description = "Recharge Request of " + amount + " TRX for user - " + username;
		}
		orderDetails.setOrderDescription(description);
		orderDetails.setUsername(username);

		return orderDetails;

	}

}
