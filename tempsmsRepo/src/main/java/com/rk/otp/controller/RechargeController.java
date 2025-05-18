package com.rk.otp.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.entity.projection.UserUtility;
import com.rk.app.persistence.repository.MetaDataRepo;
import com.rk.app.utility.Utility;
import com.rk.nowpayments.model.PaymentInfo;
import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.model.OrderRequest;
import com.rk.otp.recharge.model.OrderResponse;
import com.rk.otp.recharge.repo.OrderDetailsRepo;
import com.rk.otp.recharge.service.RechargeService;
import com.rk.otp.service.UserBalanceAuditService;
import com.rk.otp.service.UserService;

@RestController
@RequestMapping("/recharge")
public class RechargeController {

	private final MetaDataRepo metaDataRepo;
	private final OrderDetailsRepo orderDetailsRepo;
	private final UserService userService;
	private final UserBalanceAuditService userBalanceAuditService;
	private final RechargeService rechargeService;

	public RechargeController(MetaDataRepo metaDataRepo, OrderDetailsRepo orderDetailsRepo, UserService userService,
			UserBalanceAuditService userBalanceAuditService, RechargeService rechargeService) {
		this.metaDataRepo = metaDataRepo;
		this.orderDetailsRepo = orderDetailsRepo;
		this.userService = userService;
		this.userBalanceAuditService = userBalanceAuditService;
		this.rechargeService = rechargeService;
	}

	@GetMapping
	public ModelAndView showRechargePage(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("redirect:/");
		}
		String username = Utility.getAuthenticatedUserName();
		User user = userService.getProjectedUserByUsername(username).orElse(null);
		modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		modelAndView.addObject("cyptoEnabled",
				Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.crypto"));
		modelAndView.addObject("upiEnabled",
				Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.upi"));
		modelAndView.addObject("paytmEnabled",
				Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.paytm"));
		modelAndView.addObject("bharatpeEnabled",
				Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.bharatpe"));
		modelAndView.setViewName("recharge/recharge_main");
		return modelAndView;
	}

	@GetMapping("/rechargeHistory")
	public ModelAndView getRechargeHistory(Authentication authentication) {
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("redirect:/");
		}
		ModelAndView modelAndView = new ModelAndView();
		String username = Utility.getAuthenticatedUserName();
		modelAndView.addObject("rechargeHistoryList", userBalanceAuditService.findRechargeHistory(username));
		modelAndView.setViewName("recharge/recharge_history");

		return modelAndView;
	}

	@GetMapping("/paytm")
	public ModelAndView showPaytmRechargePage(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		String isEnabled = Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.paytm");
		if ((authentication == null || (authentication instanceof AnonymousAuthenticationToken))
				|| !"true".equalsIgnoreCase(isEnabled)) {
			return new ModelAndView("redirect:/");
		}
		String username = Utility.getAuthenticatedUserName();
		User user = userService.getProjectedUserByUsername(username).orElse(null);
		modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		modelAndView.setViewName("recharge/recharge_paytm");
		return modelAndView;
	}

	@PostMapping("/paytm")
	public ResponseEntity<String> processPaytmRecharge(@RequestBody Map<String, String> attributeMap) {
		if (Utility.isAnonymous()) {
			return ResponseEntity.badRequest().body("Invalid User");
		}
		String username = Utility.getAuthenticatedUserName();
		String utr = attributeMap.get("utr");
		return ResponseEntity.ok(rechargeService.processPaytmRecharge(username, utr.trim()));
	}

	@GetMapping("/bharatpe")
	public ModelAndView showBharatpeRechargePage(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		String isEnabled = Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.bharatpe");
		if ((authentication == null || (authentication instanceof AnonymousAuthenticationToken))
				|| !"true".equalsIgnoreCase(isEnabled)) {
			return new ModelAndView("redirect:/");
		}
		String username = Utility.getAuthenticatedUserName();
		User user = userService.getProjectedUserByUsername(username).orElse(null);
		modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		modelAndView.addObject("bharatpeUpi", metaDataRepo.findValueByAttribute("bharatpe.upi.id"));
		modelAndView.setViewName("recharge/recharge_bharatpe");
		return modelAndView;
	}

	@PostMapping("/bharatpe")
	public ResponseEntity<String> processBharatpeRecharge(@RequestBody Map<String, String> attributeMap) {
		if (Utility.isAnonymous()) {
			return ResponseEntity.badRequest().body("Invalid User");
		}
		String username = Utility.getAuthenticatedUserName();
		String utr = attributeMap.get("utr");
		return ResponseEntity.ok(rechargeService.processBharatpeRecharge(username, utr.trim()));
	}

	@GetMapping("/crypto")
	public ModelAndView showCryptoRechargePage(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		String isEnabled = Utility.isAdmin() ? "true" : metaDataRepo.findValueByAttribute("recharge.enabled.crypto");
		if ((authentication == null || (authentication instanceof AnonymousAuthenticationToken))
				|| !"true".equalsIgnoreCase(isEnabled)) {
			return new ModelAndView("redirect:/");
		}
		String username = Utility.getAuthenticatedUserName();
		User user = userService.getProjectedUserByUsername(username).orElse(null);
		modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		modelAndView.addObject("pendingOrdersList",
				orderDetailsRepo.findByUsernameAndStatusOrderByCreateTimeDesc(username, "I"));
		modelAndView.setViewName("recharge/recharge_crypto");
		return modelAndView;
	}

	@PostMapping("/createPayment")
	public ResponseEntity<PaymentInfo> createPayment(@RequestBody Map<String, String> attributeMap) {
		return ResponseEntity.ok(rechargeService.createPayment(attributeMap));
	}

	@GetMapping("/getPendingOrders")
	public ResponseEntity<List<OrderDetails>> getPendingOrders() {
		String username = Utility.getAuthenticatedUserName();
		List<OrderDetails> pendingOrders = orderDetailsRepo.findByUsernameAndStatusOrderByCreateTimeDesc(username, "I");
		return ResponseEntity.ok(pendingOrders);
	}

	@GetMapping("/getAllOrders")
	public ResponseEntity<List<OrderDetails>> getAllOrders() {
		String username = Utility.getAuthenticatedUserName();
		List<OrderDetails> allOrders = orderDetailsRepo.findByUsernameOrderByCreateTimeDesc(username);
		return ResponseEntity.ok(allOrders);
	}

	@GetMapping("/getRechargeUrl")
	public ResponseEntity<String> getRechargeUrl() {

		return ResponseEntity.ok().body(metaDataRepo.findValueByAttribute("mini.store.url"));
	}

	@PostMapping("/isRechargeEnabled")
	public ResponseEntity<String> isRechargeEnabled(@RequestBody String type) {

		return ResponseEntity.ok().body(metaDataRepo.findValueByAttribute("recharge.enabled." + type));
	}

	@PostMapping("/createOrder")
	public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
		try {
			orderRequest.setUsername(Utility.getAuthenticatedUserName());
			return ResponseEntity.ok(rechargeService.createOrder(orderRequest));
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body(null);
		}

	}

	@PostMapping("/check-status")
	public ResponseEntity<OrderResponse> checkStatus(@RequestBody OrderRequest orderRequest) {
		orderRequest.setUsername(Utility.getAuthenticatedUserName());
		try {
			return ResponseEntity.ok(rechargeService.checkStatus(orderRequest));
		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(OrderResponse.builder().errorMessage(e.getMessage()).build());
		}
	}

//	public static void main(String[] args) throws Exception {
//		TreeMap<String, String> params = new TreeMap<>();
//		params.put("MID", "YwpMBg20487719100799");
//		params.put("ORDERID", "T2410101425331207615131");
//
//		/**
//		 * Generate checksum by parameters we have Find your Merchant Key in your Paytm
//		 * Dashboard at https://dashboard.paytm.com/next/apikeys
//		 */
//		String paytmChecksum = PaytmChecksum.generateSignature(params, "YwpMBg2048771910");
////		boolean verifySignature = PaytmChecksum.verifySignature(params, "XjW0ki-101123456", paytmChecksum);
//		System.out.println("generateSignature Returns: " + paytmChecksum);
////		System.out.println("verifySignature Returns: " + verifySignature);
////
////		/* initialize JSON String */
////		String body = "{\"mid\":\"YwpMBg20487719100799\",\"orderId\":\"T2410101425331207615131\"}";
////
////		/**
////		 * Generate checksum by parameters we have in body Find your Merchant Key in
////		 * your Paytm Dashboard at https://dashboard.paytm.com/next/apikeys
////		 */
////		paytmChecksum = PaytmChecksum.generateSignature(body, "YwpMBg20487719100799");
////		verifySignature = PaytmChecksum.verifySignature(body, "YwpMBg20487719100799", paytmChecksum);
////		System.out.println("generateSignature Returns: " + paytmChecksum);
////		System.out.println("verifySignature Returns: " + verifySignature);
//	}

}
