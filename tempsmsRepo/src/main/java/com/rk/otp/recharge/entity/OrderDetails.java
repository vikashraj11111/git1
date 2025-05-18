package com.rk.otp.recharge.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

import com.rk.otp.constants.AppConstants;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;
import lombok.ToString;

@Entity
@Data
@ToString
public class OrderDetails {

	@Id
	@Column(unique = true, nullable = false)
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long orderId;
	private String orderDescription;
	private Double amount;
	private String currency;
	private String username;
	private LocalDateTime createTime;
	private LocalDateTime updateTime;
	private boolean isSuccess;
	private String status;
	private String paymentId;
	private String payAddress;
	private String paymentMethod;

	@PrePersist
	protected void onCreate() {
		if (createTime == null)
			createTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
		if (status == null) {
			this.status = AppConstants.STATUS_IN_PROGRESS;
		}
		if (currency == null) {
			this.currency = AppConstants.CURRENCY_INR;
		}
		if (paymentMethod == null) {
			this.paymentMethod = AppConstants.PAYMENT_METHOD_PAYTM;
		}
		isSuccess = false;
	}

	@PreUpdate
	protected void onUpdate() {
		updateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
	}

}
