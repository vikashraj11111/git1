package com.rk.otp.tasks;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.User;
import com.rk.nowpayments.model.PaymentInfo;
import com.rk.nowpayments.service.NowPaymentsService;
import com.rk.otp.constants.AppConstants;
import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.repo.OrderDetailsRepo;
import com.rk.otp.recharge.service.RechargeService;
import com.rk.otp.service.UserService;

@Component
@EnableAsync
public class RechargeTask {

	private static final Logger LOGGER = LoggerFactory.getLogger(RechargeTask.class);

	private final NowPaymentsService nowPaymentsService;

	private final OrderDetailsRepo orderDetailsRepo;

	private final UserService userService;

	private final CustomMailSender customMailSender;

	private final RechargeService rechargeService;

	public RechargeTask(NowPaymentsService nowPaymentsService, OrderDetailsRepo orderDetailsRepo,
			UserService userService, CustomMailSender customMailSender, RechargeService rechargeService) {
		this.nowPaymentsService = nowPaymentsService;
		this.orderDetailsRepo = orderDetailsRepo;
		this.userService = userService;
		this.customMailSender = customMailSender;
		this.rechargeService = rechargeService;
	}

	@Scheduled(fixedDelay = 10000)
	@Async
	public void updateRechargeStatus() {
		List<OrderDetails> unProcessedRechargeList = orderDetailsRepo.findByStatusAndPaymentMethodOrderByCreateTime(
				AppConstants.STATUS_IN_PROGRESS, AppConstants.PAYMENT_METHOD_NOWPAYMENTS);
		LOGGER.debug("RechargeTask :: unProcessedRechargeList :: {}", unProcessedRechargeList);
		unProcessedRechargeList.parallelStream().forEach(this::updatePaymentStatus);
	}

	@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
	@Async
	public void updateFailedOrders() {
		List<OrderDetails> unProcessedRechargeList = orderDetailsRepo.findByStatusOrderByCreateTime("I");
		unProcessedRechargeList.parallelStream().filter(this::isMoreThanOneDayOld).forEach(this::updatePendingToFailed);
	}

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Kolkata")
	@Async
	public void updateFailedBharatPeOrders() {
		List<OrderDetails> unProcessedRechargeList = orderDetailsRepo.findByStatusAndPaymentMethodOrderByCreateTime(
				AppConstants.STATUS_IN_PROGRESS, AppConstants.PAYMENT_METHOD_BHARATPE);
		unProcessedRechargeList.parallelStream().filter(this::isMoreThan10MinsOld).forEach(this::updatePendingToFailed);
	}

	@Scheduled(fixedDelay = 5000)
	@Async
	public void checkBharatpeRechargeStatus() {
		List<OrderDetails> unProcessedRechargeList = orderDetailsRepo.findByStatusAndPaymentMethodOrderByCreateTime(
				AppConstants.STATUS_IN_PROGRESS, AppConstants.PAYMENT_METHOD_BHARATPE);
		LOGGER.info("RechargeTask :: checkBharatpeRechargeStatus() :: {}", unProcessedRechargeList);
		unProcessedRechargeList.parallelStream().forEach(rechargeService::checkAndUpdateBharatPeStatus);
	}

	private boolean isMoreThanOneDayOld(OrderDetails orderDetails) {
		return LocalDateTime.now(ZoneId.of("Asia/Kolkata")).isAfter(orderDetails.getCreateTime().plusDays(1));
	}

	private boolean isMoreThan10MinsOld(OrderDetails orderDetails) {
		return LocalDateTime.now(ZoneId.of("Asia/Kolkata")).isAfter(orderDetails.getCreateTime().plusMinutes(10));
	}

	private void updatePendingToFailed(OrderDetails orderDetails) {
		orderDetails.setStatus("F");
		orderDetails.setSuccess(false);
		orderDetailsRepo.saveAndFlush(orderDetails);
	}

	private void updatePaymentStatus(OrderDetails orderDetails) {
		String paymentId = orderDetails.getPaymentId();
		PaymentInfo paymentInfo = nowPaymentsService.getPaymentStatus(paymentId);
		String paymentStatus = paymentInfo.getPaymentStatus();
		if ("waiting".equalsIgnoreCase(paymentStatus)) {
			return;
		} else if ("expired".equalsIgnoreCase(paymentStatus)) {
			orderDetails.setStatus("F");
			orderDetails.setSuccess(false);
		} else {
			double paidAmountDifference = paymentInfo.getPayAmount() - paymentInfo.getActuallyPaid();
			if (paidAmountDifference <= 0.1) {
				orderDetails.setStatus("S");
				orderDetails.setSuccess(true);
				updateUserBalance(orderDetails);

			} else {
				LOGGER.debug(
						"RechargeTask :: updatePaymentStatus :: user did not pay actual amount :: paid a diff of {}",
						paidAmountDifference);
				orderDetails.setStatus("U");
				orderDetails.setSuccess(false);
			}

		}

		orderDetailsRepo.saveAndFlush(orderDetails);
	}

	private void updateUserBalance(OrderDetails orderDetails) {
		Double amount = orderDetails.getAmount();
		if ("TRX".equalsIgnoreCase(orderDetails.getCurrency())) {
			amount = amount * 10;
		}
		User user = userService.addBalance(orderDetails.getUsername(), amount, "RECHARGE",
				"CRYPTO-" + orderDetails.getPaymentId());
		String userEmail = user.getEmail();
		if (userEmail != null) {
			customMailSender.sendEmail(userEmail, "Recharge Successful of Rs. " + orderDetails.getAmount(),
					createRechargeSuccessMail(orderDetails).toString());
		}
		mailAdmin(orderDetails);

	}

	private void mailAdmin(OrderDetails orderDetails) {
		customMailSender.sendEmailToAdmin("Recharge Successful of Rs. " + orderDetails.getAmount(),
				createRechargeSuccessMailAdmin(orderDetails));
	}

	private StringBuilder createRechargeSuccessMail(OrderDetails orderDetails) {
		StringBuilder sb = new StringBuilder();
		sb.append("Recharge Success of Rs. ").append(orderDetails.getAmount()).append("!!").append("\n\n")
				.append("Recharge amount has been added to the account with username: ")
				.append(orderDetails.getUsername());

		return sb;
	}

	private String createRechargeSuccessMailAdmin(OrderDetails orderDetails) {
		return createRechargeSuccessMail(orderDetails).append("\n\n").append("OrderReference: ").append("CRYPTO-")
				.append(orderDetails.getPaymentId()).toString();
	}

}
