package com.rk.otp.recharge.service;

import java.io.IOException;

import com.rk.otp.recharge.model.OrderResponse;

public interface PaymentService {

	double getUniqueAmount(double amount) throws RuntimeException;

	void generateQrCode(Long userId, OrderResponse orderResponse) throws IOException;

}
