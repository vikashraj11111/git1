package com.rk.otp.recharge.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.repo.OrderDetailsRepo;
import com.rk.otp.recharge.service.OrderDetailsService;

@Transactional(propagation = Propagation.REQUIRES_NEW)
@Service
public class OrderDetailsServiceImpl implements OrderDetailsService {

	private final OrderDetailsRepo orderDetailsRepo;

	public OrderDetailsServiceImpl(OrderDetailsRepo orderDetailsRepo) {
		this.orderDetailsRepo = orderDetailsRepo;
	}
	
	@Override
	public void saveOrder(OrderDetails orderDetails) {
		orderDetailsRepo.saveAndFlush(orderDetails);
	}
	
	@Override
	public void deleteOrder(OrderDetails orderDetails) {
		orderDetailsRepo.delete(orderDetails);
	}
	
	@Override
	public List<OrderDetails> findOrder(String paymentId) {
		return orderDetailsRepo.findByPaymentId(paymentId);
	}
	
	@Override
	public Optional<OrderDetails> findOrderById(Long orderId) {
		return orderDetailsRepo.findById(orderId);
	}
	
	@Override
	public List<OrderDetails> getAllOrders() {
		return orderDetailsRepo.findAll();
	}
	
	@Override
	public List<OrderDetails> getSortedOrders() {
		return orderDetailsRepo.findAllByOrderByCreateTimeDesc();
	}
	
	@Override
	public Optional<List<OrderDetails>> searchFirstTenOrders(String keyword) {
		return orderDetailsRepo.findFirst10ByPaymentIdContainingIgnoreCaseOrderByCreateTimeDesc(keyword);
	}

}
