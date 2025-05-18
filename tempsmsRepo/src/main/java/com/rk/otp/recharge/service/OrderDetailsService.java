package com.rk.otp.recharge.service;

import java.util.List;
import java.util.Optional;

import com.rk.otp.recharge.entity.OrderDetails;

public interface OrderDetailsService {

	void saveOrder(OrderDetails orderDetails);

	void deleteOrder(OrderDetails orderDetails);

	List<OrderDetails> findOrder(String paymentId);

	List<OrderDetails> getAllOrders();

	List<OrderDetails> getSortedOrders();

	Optional<List<OrderDetails>> searchFirstTenOrders(String keyword);

	Optional<OrderDetails> findOrderById(Long orderId);

}
