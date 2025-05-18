package com.rk.otp.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.Services;
import com.rk.app.user.UserDetailsImpl;
import com.rk.otp.service.ServicesService;

@RestController
@RequestMapping("/public")
public class PublicController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PublicController.class);
	
	@Value("${telegram.admin.url.1}")
	private String TELEGRAM_ADMIN_URL;
	@Value("${telegram.admin.url.2}")
	private String TELEGRAM_ADMIN_2_URL;
	
	@Value("${spring.application.name}")
	private String appName;
	
	@Autowired
	private CustomMailSender mailSender;
	
	@Autowired
	private ServicesService servicesService;
	
	@GetMapping("/tnc")
	public ModelAndView termsNConditions() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("public/tnc");
		return modelAndView;
	}
	
	@GetMapping("/aboutus")
	public ModelAndView aboutUs() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("public/about-us");
		return modelAndView;
	}
	
	@GetMapping("/contactus")
	public ModelAndView contactUs() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("public/contact-us");
		return modelAndView;
	}
	
	@GetMapping("/faq")
	public ModelAndView faq() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("public/faq");
		return modelAndView;
	}
	
	@PostMapping("/contactus")
	public String contactUsProcess(Authentication authentication, @RequestBody Map<String, String> attributeMap) {
		String user = "Guest";
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			user = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		}
		
		String name = attributeMap.get("name");
		String email = attributeMap.get("email");
		String msg = attributeMap.get("msg");
		try {
			mailSender.sendContactUsReportHtml(user, name, email, msg);
		} catch (Exception e) {
			return "ERROR";
		}
		
		try {
			mailSender.sendEmail(email, "Message received",
					"Thank you for your query. We will get back to you shortly.\n\nTeam " + appName + ".");
		} catch (Exception e) {
			LOGGER.error("contactus :: error sending mail :: {}", e);
		}
		
		return "Thank you for your message.";
	}
	
	@GetMapping("/maintenance")
	public ModelAndView maintenance() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("public/maintenance");
		return modelAndView;
	}
	
	@GetMapping("/tg/admin/{adminNumber}")
	public ModelAndView redirectAdminTG(@PathVariable String adminNumber) {
		switch(adminNumber) {
		case "1":
			return new ModelAndView("redirect:" + TELEGRAM_ADMIN_URL);
		case "2":
			return new ModelAndView("redirect:" + TELEGRAM_ADMIN_2_URL);
		default:
			return new ModelAndView("redirect:" + TELEGRAM_ADMIN_URL);
		}
		
	}
	
	@GetMapping("/priceList")
	public ModelAndView showPriceListPage() {
		ModelAndView modelAndView = new ModelAndView();
		List<Services> activeServicesList = servicesService.getActiveServices();
		
		final Map<String, Set<Double>> servicePriceMap = activeServicesList.stream()
				.collect(Collectors.groupingBy(s -> s.getServiceName(),
						LinkedHashMap::new, Collectors.mapping(s -> s.getPrice(), Collectors.toSet())));
				
		modelAndView.addObject("servicePriceMap", servicePriceMap);
		modelAndView.setViewName("public/price_list");
		return modelAndView;
	}
}
