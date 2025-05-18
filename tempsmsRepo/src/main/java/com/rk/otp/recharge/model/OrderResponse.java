package com.rk.otp.recharge.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class OrderResponse {
	
	private Long orderId;
	private Double amount;
	private String username;
	private LocalDateTime createTime;
	private Boolean isSuccess;
	private String status;
	private String paymentMethod;
	private String qrCode;
	private String qrLink;
	private Long remainingTime;
	private String errorMessage;
}
