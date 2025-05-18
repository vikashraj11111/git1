package com.rk.otp.recharge.service.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.repository.MetaDataRepo;
import com.rk.nowpayments.model.PaymentInfo;
import com.rk.nowpayments.service.NowPaymentsService;
import com.rk.otp.constants.AppConstants;
import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.model.OrderRequest;
import com.rk.otp.recharge.model.OrderResponse;
import com.rk.otp.recharge.model.PaytmResponse;
import com.rk.otp.recharge.service.OrderDetailsService;
import com.rk.otp.recharge.service.PaymentService;
import com.rk.otp.recharge.service.RechargeService;
import com.rk.otp.service.UserService;
import com.rk.otp.utility.AppUtility;

@Service
public class RechargeServiceImpl implements RechargeService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RechargeServiceImpl.class);

	@Value("${paytm.recharge.verify.url}")
	private String paytmRechargeValidateUrl;

	@Value("${bharatpe.validate.url}")
	private String bharatpeUrl;

	private final RestTemplate restTemplate;
	private final MetaDataRepo metaDataRepo;
	private final UserService userService;
	private final OrderDetailsService orderDetailsService;
	private final NowPaymentsService nowPaymentsService;
	private final WebClient webClient;
	private final PaymentService paymentService;

	public RechargeServiceImpl(RestTemplate restTemplate, MetaDataRepo metaDataRepo, UserService userService,
			OrderDetailsService orderDetailsService, NowPaymentsService nowPaymentsService, WebClient webClient,
			PaymentService paymentService) {
		this.restTemplate = restTemplate;
		this.metaDataRepo = metaDataRepo;
		this.userService = userService;
		this.orderDetailsService = orderDetailsService;
		this.nowPaymentsService = nowPaymentsService;
		this.webClient = webClient;
		this.paymentService = paymentService;
	}

	@Override
	public PaymentInfo createPayment(Map<String, String> attributeMap) {
		String amount = attributeMap.get("amount");
		String currency = attributeMap.get("currency");
		OrderDetails orderDetails = AppUtility.createOrder(amount, currency);
		PaymentInfo paymentInfo = nowPaymentsService.createPayment(amount, String.valueOf(orderDetails.getOrderId()),
				orderDetails.getOrderDescription(), false, false, currency);
		orderDetails.setPaymentId(paymentInfo.getPaymentId());
		orderDetails.setPayAddress(paymentInfo.getPayAddress());
		orderDetails.setPaymentMethod(AppConstants.PAYMENT_METHOD_NOWPAYMENTS);
		orderDetailsService.saveOrder(orderDetails);
		return paymentInfo;
	}

	/*********************************** PAYTM ***********************************/

	@Override
	public String processPaytmRecharge(String username, String utr) {

		if (!orderDetailsService.findOrder(utr).isEmpty()) {
			return "ALREADY_USED";
		}
		OrderDetails orderDetails = createOrder(username, utr, AppConstants.PAYMENT_METHOD_PAYTM);
		try {
			String mid = metaDataRepo.findValueByAttribute("paytm.merchant.id");
			PaytmResponse response;
			response = restTemplate.getForObject(String.format(paytmRechargeValidateUrl, mid, utr),
					PaytmResponse.class);

			if (response == null) {
				return AppConstants.FAILED;
			}
			if ("TXN_FAILURE".equalsIgnoreCase(response.getStatus())) {
				orderDetailsService.deleteOrder(orderDetails);
				return "NOT_FOUND";
			} else if ("TXN_SUCCESS".equalsIgnoreCase(response.getStatus())) {
				double rechargeValue = Double.parseDouble(response.getTxnAmount());
				String orderReference = "PAYTM-" + utr;
				User user = userService.addBalance(username, rechargeValue, "RECHARGE", orderReference);

				orderDetails.setAmount(rechargeValue);
				orderDetails.setSuccess(true);
				orderDetails.setStatus("S");
				orderDetails.setPayAddress(response.getMid());
				orderDetails.setOrderDescription("Paytm payment of Rs. " + rechargeValue + " was received on "
						+ response.getTxnDate() + " via " + response.getPaymentMode());

				orderDetailsService.saveOrder(orderDetails);
				return String.valueOf(user.getBalance());
			}
		} catch (Exception e) {
			LOGGER.error("Exception while fetching paytm transaction details for UTR {} :: {}", utr, e.getMessage(), e);
			orderDetailsService.deleteOrder(orderDetails);
		}

		return AppConstants.FAILED;
	}

	/*********************************
	 * PAYTM END
	 *************************************/

	/************************ BHARATPE *****************************/

	@Override
	public String processBharatpeRecharge(String username, String utr) {
		if (!orderDetailsService.findOrder(utr).isEmpty()) {
			return "ALREADY_USED";
		}
		OrderDetails orderDetails = createOrder(username, utr, AppConstants.PAYMENT_METHOD_BHARATPE);

		try {
			JSONObject jsonObject = getTransactionDetailsByUtr(utr);

			if (jsonObject != null) {
				Double amount = jsonObject.getDouble("amount");
				Long paymentTimestamp = jsonObject.getLong("paymentTimestamp");
				String orderReference = "BHARATPE-" + utr;
				User user = userService.addBalance(username, amount, "RECHARGE", orderReference);

				orderDetails.setAmount(amount);
				orderDetails.setSuccess(true);
				orderDetails.setStatus("S");
				orderDetails.setPayAddress(null);
				orderDetails.setOrderDescription(
						"Bharatpe payment of Rs. " + amount + " was received on " + paymentTimestamp);

				orderDetailsService.saveOrder(orderDetails);
				return String.valueOf(user.getBalance());
			} else {
				orderDetailsService.deleteOrder(orderDetails);
				return "NOT_FOUND";
			}
		} catch (Exception e) {
			LOGGER.error("Exception while fetching bharatpe transaction details for UTR {} :: {}", utr, e.getMessage(),
					e);
		}

		orderDetailsService.deleteOrder(orderDetails);

		return AppConstants.FAILED;
	}

	private JSONObject getTransactionDetailsByUtr(String utr) {
		String response = getAllWithin1Day();

		JSONObject jsonObject = new JSONObject(response);
		JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("transactions");
		Iterator<Object> iterator = jsonArray.iterator();
		while (iterator.hasNext()) {
			JSONObject next = (JSONObject) iterator.next();

			String transactionId = next.getString("bankReferenceNo");
			if (utr.equalsIgnoreCase(transactionId) && next.getString("status").equalsIgnoreCase("SUCCESS")) {
				return next;
			}
		}
		return null;
	}

	private JSONObject getTransactionDetailsByAmount(Double amount) {
		String response = getAllWithin10Minutes();

		JSONObject jsonObject = new JSONObject(response);
		JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("transactions");
		Iterator<Object> iterator = jsonArray.iterator();
		while (iterator.hasNext()) {
			JSONObject next = (JSONObject) iterator.next();
			Double amount2 = next.getDouble("amount");
			if (amount2.equals(amount) && next.getString("status").equalsIgnoreCase("SUCCESS")) {
				return next;
			}
		}
		return null;
	}

	private String getAllWithin1Day() {
		String token = metaDataRepo.findValueByAttribute("bharatpe.merchant.token");
		ResponseEntity<String> response = webClient.get()
				.uri(String.format(bharatpeUrl, getStartTime(1440), getEndTime()))
				.headers(headers -> headers.add("Token", token)).retrieve().toEntity(String.class)
				.timeout(Duration.ofMillis(2000L)).retry(2).block();

		LOGGER.info("Bharatpe Response :: {}", response);

		return response != null ? response.getBody() : null;
	}

	private String getAllWithin10Minutes() {
		String token = metaDataRepo.findValueByAttribute("bharatpe.merchant.token");
		ResponseEntity<String> response = webClient.get()
				.uri(String.format(bharatpeUrl, getStartTime(10), getEndTime()))
				.headers(headers -> headers.add("Token", token)).retrieve().toEntity(String.class)
				.timeout(Duration.ofMillis(2000L)).retry(2).block();

		return response != null ? response.getBody() : null;
	}

	private Long getStartTime(int min) {
		return LocalDateTime.now(ZoneId.of("UTC")).minusMinutes(min).toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	private Long getEndTime() {
		return LocalDateTime.now(ZoneId.of("UTC")).toInstant(ZoneOffset.UTC).toEpochMilli();
	}

	/*****************************
	 * BHARATPE END
	 **************************************/

	private OrderDetails createOrder(String username, String utr, String paymentMethod) {
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setUsername(username);
		orderDetails.setCreateTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		orderDetails.setPaymentId(utr);
		orderDetails.setPaymentMethod(paymentMethod);

		orderDetailsService.saveOrder(orderDetails);
		return orderDetails;
	}

	@Override
	public OrderDetails getOrderDetailsByUtr(String utr) {
		List<OrderDetails> orderDetailsList = orderDetailsService.findOrder(utr);
		return orderDetailsList.isEmpty() ? null : orderDetailsList.get(0);
	}

	@Override
	public List<OrderDetails> getAllOrders() {
		return orderDetailsService.getAllOrders();
	}

	@Override
	public Page<OrderDetails> getPaginatedOrders(Pageable pageable) {
		int pageSize = pageable.getPageSize();
		int currentPage = pageable.getPageNumber();
		int startItem = currentPage * pageSize;
		List<OrderDetails> orderDetailsList = orderDetailsService.getSortedOrders();
		List<OrderDetails> list = orderDetailsList.stream().skip(startItem).limit(pageSize).toList();

		return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), orderDetailsList.size());
	}

	@Override
	public List<OrderDetails> searchOrdersByKeyword(String keyword) {

		return orderDetailsService.searchFirstTenOrders(keyword).orElse(null);
	}

	@Override
	public OrderResponse createOrder(OrderRequest orderRequest) throws IOException {
		String paymentMethod = orderRequest.getPaymentMethod();
		String username = orderRequest.getUsername();
		Long userId = userService.getUser(username).getId();

		Double amount = orderRequest.getAmount();
		amount = paymentService.getUniqueAmount(amount);
		OrderResponse orderResponse = OrderResponse.builder()
				.amount(amount)
				.isSuccess(false)
				.paymentMethod(paymentMethod)
				.username(username)
				.status(AppConstants.STATUS_IN_PROGRESS)
				.remainingTime(600L)
				.build();

		paymentService.generateQrCode(userId, orderResponse);

		OrderDetails orderDetails = createOrder(orderResponse);
		orderResponse.setCreateTime(orderDetails.getCreateTime());
		orderResponse.setOrderId(orderDetails.getOrderId());
		
		return orderResponse;
	}

	private OrderDetails createOrder(OrderResponse orderResponse) {
		OrderDetails orderDetails = new OrderDetails();
		orderDetails.setUsername(orderResponse.getUsername());
		orderDetails.setCreateTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		orderDetails.setPaymentMethod(orderResponse.getPaymentMethod());
		orderDetails.setAmount(orderResponse.getAmount());
		orderDetails.setOrderDescription(String.format("Recharge Request of amount %s received on %s",
				orderResponse.getAmount(), orderDetails.getCreateTime()));

		orderDetailsService.saveOrder(orderDetails);
		return orderDetails;
	}

	@Override
	public OrderResponse checkStatus(OrderRequest orderRequest) {
		Long orderId = orderRequest.getOrderId();
		Optional<OrderDetails> optionalOrderDetails = orderDetailsService.findOrderById(orderId);
		OrderResponse orderResponse = OrderResponse.builder().amount(orderRequest.getAmount()).orderId(orderId)
				.paymentMethod(orderRequest.getPaymentMethod()).build();

		if (optionalOrderDetails.isPresent()) {
			OrderDetails orderDetails = optionalOrderDetails.get();
			String status = orderDetails.getStatus();
			orderResponse.setUsername(orderDetails.getUsername());
			long elapsedTime = ChronoUnit.SECONDS.between(orderDetails.getCreateTime(),
					LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
			orderResponse.setRemainingTime(600 - elapsedTime);

			if (status.equals(AppConstants.STATUS_FAILED)) {
				orderResponse.setStatus(AppConstants.STATUS_FAILED);
				orderResponse.setIsSuccess(false);
			} else if (status.equalsIgnoreCase(AppConstants.STATUS_SUCCESS)) {
				orderResponse.setStatus(AppConstants.STATUS_SUCCESS);
				orderResponse.setIsSuccess(true);
			} else if (elapsedTime > 600 || status.equalsIgnoreCase(AppConstants.STATUS_CANCELLED)) {
				orderResponse.setStatus(AppConstants.STATUS_CANCELLED);
				orderResponse.setIsSuccess(false);
			} else if (status.equalsIgnoreCase(AppConstants.STATUS_IN_PROGRESS)) {
				orderResponse.setStatus(AppConstants.STATUS_IN_PROGRESS);
				orderResponse.setIsSuccess(false);
			}

		} else {
			orderResponse.setErrorMessage("Order Not Found");
		}

		return orderResponse;
	}

	@Override
	public boolean checkAndUpdateBharatPeStatus(OrderDetails orderDetails) {

		//Adding for verifying again
		orderDetails = orderDetailsService.findOrderById(orderDetails.getOrderId()).orElse(null);
		if (orderDetails != null && AppConstants.STATUS_IN_PROGRESS.equals(orderDetails.getStatus())) {
			Double amount = orderDetails.getAmount();
			String username = orderDetails.getUsername();
			try {
				JSONObject jsonObject = getTransactionDetailsByAmount(amount);

				if (jsonObject != null) {
					Long paymentTimestamp = jsonObject.getLong("paymentTimestamp");
					String utr = jsonObject.getString("bankReferenceNo");
					String orderReference = "BHARATPE-" + utr;
					userService.addBalance(username, amount, "RECHARGE", orderReference);

					orderDetails.setPaymentId(utr);
					orderDetails.setSuccess(true);
					orderDetails.setStatus(AppConstants.STATUS_SUCCESS);
					orderDetails.setPayAddress(null);
					orderDetails.setOrderDescription(
							"Bharatpe payment of Rs. " + amount + " was received on " + paymentTimestamp);

					orderDetailsService.saveOrder(orderDetails);
					return true;
				}
			} catch (Exception e) {
				LOGGER.error("Exception while fetching bharatpe transaction details for amount {} :: {}", amount,
						e.getMessage(), e);
			}
		}
		return false;
	}

}
