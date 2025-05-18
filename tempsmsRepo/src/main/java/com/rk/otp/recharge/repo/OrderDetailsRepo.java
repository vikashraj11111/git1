package com.rk.otp.recharge.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rk.otp.recharge.entity.OrderDetails;

public interface OrderDetailsRepo extends JpaRepository<OrderDetails, Long> {
	
	List<OrderDetails> findByPaymentId(String paymentId);
	
	List<OrderDetails> findByStatusOrderByCreateTime(String status);
	
	List<OrderDetails> findByStatusAndPaymentMethodOrderByCreateTime(String status, String paymentMethod);
	
	List<OrderDetails> findByUsernameOrderByCreateTimeDesc(String username);
	
	List<OrderDetails> findByUsernameAndStatusOrderByCreateTimeDesc(String username, String status);

	Optional<List<OrderDetails>> findFirst10ByPaymentIdContainingIgnoreCaseOrderByCreateTimeDesc(String keyword);

	List<OrderDetails> findAllByOrderByCreateTimeDesc();

	List<OrderDetails> findByAmountAndStatus(double amount, String status);

}
