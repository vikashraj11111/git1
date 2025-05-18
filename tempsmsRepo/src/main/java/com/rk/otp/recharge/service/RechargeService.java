package com.rk.otp.recharge.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rk.nowpayments.model.PaymentInfo;
import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.model.OrderRequest;
import com.rk.otp.recharge.model.OrderResponse;

public interface RechargeService {

	String processPaytmRecharge(String username, String utr);

	PaymentInfo createPayment(Map<String, String> attributeMap);

	String processBharatpeRecharge(String username, String utr);

	OrderDetails getOrderDetailsByUtr(String utr);

	List<OrderDetails> getAllOrders();

	Page<OrderDetails> getPaginatedOrders(Pageable pageable);

	List<OrderDetails> searchOrdersByKeyword(String keyword);

	OrderResponse createOrder(OrderRequest orderRequest) throws IOException;

	OrderResponse checkStatus(OrderRequest orderRequest);

	boolean checkAndUpdateBharatPeStatus(OrderDetails orderDetails);

}
