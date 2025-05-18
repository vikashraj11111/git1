package com.rk.otp.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.user.UserDetailsImpl;
import com.rk.app.utility.Utility;
import com.rk.otp.service.CodeNumberService;
import com.rk.otp.service.OtpService;
import com.rk.otp.service.ServicesService;
import com.rk.otp.service.UserService;

@RestController
@RequestMapping("/otp")
public class OtpController {

	@Autowired
	private ServicesService servicesService;

	@Autowired
	private UserService userService;

	@Autowired
	private CodeNumberService codeNumberService;

	@Autowired
	private OtpService otpService;

	private static final List<String> RESEND_ENABLED_SERVERS = Arrays.asList("server2", "server3", "server4");

	@GetMapping("/{service}")
	public ModelAndView showOtpPageForService(@PathVariable String service,
			Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		String server = servicesService.getServerFromService(service);

		if (!userService.isBalanceAvailable(authentication, service, server))
			modelAndView.addObject("error", "INSUFFICIENT_BALANCE");

		modelAndView.addObject("app", service);
		String appName = servicesService.getServiceName(service);

		if ("NOT_FOUND".equalsIgnoreCase(appName))
			modelAndView.addObject("error", "INVALID APP NAME");
		else if (appName.toLowerCase().contains("flipkart"))
			modelAndView.addObject("appName", "Flipkart");
		else
			modelAndView.addObject("appName", appName);

		if (RESEND_ENABLED_SERVERS.contains(server))
			modelAndView.addObject("resendEnabled", "true");

		if (Utility.isAdmin(authentication) && "Simpl".equalsIgnoreCase(appName))
			modelAndView.addObject("registrationCheck", "true");
		else
			modelAndView.addObject("registrationCheck", Utility.isRegistrationCheckAvailable(appName));

		modelAndView.setViewName("otp_page");
		return modelAndView;
	}

	@GetMapping("/activeNumber")
	public ModelAndView showOtpPageForService(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		CodeNumber latestActiveNumber = codeNumberService.getLatestActiveNumber(authentication);
		modelAndView.addObject("app", latestActiveNumber.getService());
		String appName = servicesService.getServiceName(latestActiveNumber.getService());
		modelAndView.addObject("appName", appName);
		String server = latestActiveNumber.getServer();
		if (RESEND_ENABLED_SERVERS.contains(server))
			modelAndView.addObject("resendEnabled", "true");
		modelAndView.addObject("code", latestActiveNumber.getCode());
		modelAndView.addObject("number", latestActiveNumber.getNumber());
		modelAndView.addObject("activeService", latestActiveNumber.getService());

		if (Utility.isAdmin(authentication) && "Simpl".equalsIgnoreCase(appName))
			modelAndView.addObject("registrationCheck", "true");
		else
			modelAndView.addObject("registrationCheck", Utility.isRegistrationCheckAvailable(appName));

		modelAndView.setViewName("otp_page");
		return modelAndView;
	}

	@GetMapping("/{service}/price")
	public String getPriceOfService(@PathVariable String service, Authentication authentication) {
		String server = servicesService.getServerFromService(service);

		if (!userService.isBalanceAvailable(authentication, service, server)) {
			return "INSUFFICIENT_BALANCE";
		}

		return servicesService.getPrice(service).toString();
	}

	@GetMapping("/{service}/getnumber")
	public String getNumber(@PathVariable String service, Authentication authentication) {
		synchronized (this) {
			String server = servicesService.getServerFromService(service);
//			System.out.println("inside the synchorized block of getNumber() :: " + Thread.currentThread().getName());
			if (codeNumberService.activeNumberCount(authentication) >= 5) {
				return "ALREADY_ACTIVE";
			}

			if (!userService.isBalanceAvailable(authentication, service, server)) {
				return "INSUFFICIENT_BALANCE";
			}
			Double amount = servicesService.findById(service).get().getPrice();
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getUsername();

			String codeNumber = otpService.getCodeNumber(amount, server, service, username, 0);

			return codeNumber;
		}
	}

	@GetMapping("/getotp/{code}")
	public String getOtp(@PathVariable String code, Authentication authentication) {
//		synchronized (authentication) {
		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		return getOtpByCode(code, username, authentication);
//		}
	}

	@GetMapping("/resend/{code}")
	public boolean resendOtp(@PathVariable String code, Authentication authentication) {
		return otpService.resend(code, authentication);
	}

	@GetMapping("/cancel/{code}")
	public boolean cancelActivation(@PathVariable String code, Authentication authentication) {
		synchronized (authentication) {
			return otpService.cancelActivation(code, authentication);
		}
	}

	@GetMapping("/cancelActive")
	public boolean cancelActiveNumber(Authentication authentication) {
		synchronized (authentication) {
			try {
				List<CodeNumber> activeCodeNumbers = codeNumberService.findActiveNumberByAuthentication(authentication);
				activeCodeNumbers.stream().map(c -> c.getCode()).forEach(code -> {
					otpService.cancelActivation(code, authentication);
				});
			} catch (Exception e) {
				return false;
			}
		}

		return true;
	}

	/*********************************************************************************************************
	 * 
	 *********************************************************************************************************/

	private String getOtpByCode(String code, String username, Authentication authentication) {
		Optional<CodeNumber> optionalCodeNumber = codeNumberService.findById(code);
		if (optionalCodeNumber.isEmpty())
			return "CODE_NOT_FOUND";

		CodeNumber codeNumber = optionalCodeNumber.get();
		String server = codeNumber.getServer();
		String service = codeNumber.getService();
		if (codeNumber.getBalanceDeducted().equalsIgnoreCase("N")
				&& !userService.isBalanceAvailable(authentication, service, server)) {
			return "INSUFFICIENT_BALANCE";
		}

		return otpService.getOtpByCode(service, server, code, username, codeNumber, Utility.isAdmin(authentication));
	}

}
