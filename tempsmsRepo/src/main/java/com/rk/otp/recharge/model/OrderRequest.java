package com.rk.otp.recharge.model;

import lombok.Data;

@Data
public class OrderRequest {

	private Long orderId;
	private Double amount;
	private String username;
	private String paymentMethod;
}
