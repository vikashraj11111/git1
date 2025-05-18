package com.rk.otp.controller;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.rk.app.persistence.entity.Announcement;
import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.Services;
import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.entity.UsersOtpHistory;
import com.rk.app.persistence.entity.projection.UserUtility;
import com.rk.app.user.UserDetailsImpl;
import com.rk.app.utility.Utility;
import com.rk.otp.service.AnnouncementService;
import com.rk.otp.service.CodeNumberService;
import com.rk.otp.service.ServerKeysService;
import com.rk.otp.service.ServicesService;
import com.rk.otp.service.UserService;
import com.rk.otp.service.UsersOtpHistoryService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private ServicesService servicesService;

	@Autowired
	private UsersOtpHistoryService otpHistoryService;

	@Autowired
	private AnnouncementService announcementService;

	@Autowired
	private CodeNumberService codeNumberService;

	@Autowired
	private ServerKeysService serverKeysService;

	@GetMapping("/otp")
	public ModelAndView showOtpPageByService(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			User user = userService.getProjectedUser(id);
			modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));

			List<String> allEnabledServers = serverKeysService.findAllEnabledServers();
			List<Services> activeServicesList = servicesService.getActiveServices().stream()
					.filter(s -> allEnabledServers.contains(s.getServer())).collect(Collectors.toList());
			final Map<String, TreeSet<Services>> activeServicesMap = activeServicesList.stream()
					.collect(Collectors.groupingBy(
							s -> s.getCode().contains("new") ? s.getServiceList().getServiceId() + "_new"
									: s.getCode().contains("old") ? s.getServiceList().getServiceId() + "_old"
											: s.getServiceList().getServiceId().toString(),
							LinkedHashMap::new, Collectors.mapping(s -> s, Collectors.toCollection(() -> new TreeSet<>(
									Comparator.comparing(Services::getPrice).thenComparing(Services::getCode))))));

			modelAndView.addObject("activeServicesMap", activeServicesMap);

			modelAndView.addObject("activeNumber", codeNumberService.haveActiveNumber(authentication));
		}
		modelAndView.setViewName("otp_service");
		return modelAndView;
	}

	@GetMapping("/dashboard")
	public ModelAndView getUserDashboard(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("redirect:/");
		}
		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();

		List<CodeNumber> allNumbers = userService.getAllNumbers(username);
		modelAndView.addObject("codeNumberList", allNumbers);

		User user = userService.getProjectedUserByUsername(username).get();
		modelAndView.addObject("user", user);

		List<Announcement> announcements = announcementService.getAnnouncements();
		modelAndView.addObject("announcements", announcements);

		modelAndView.setViewName("user_dashboard");
		return modelAndView;
	}

	@GetMapping("/addEmail")
	public ModelAndView showAddEmailPage(Authentication authentication) {
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("redirect:/login");
		}
		ModelAndView modelAndView = new ModelAndView();
		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		User user = userService.getUser(username);
		if (StringUtils.hasText(user.getEmail()))
			modelAndView.addObject("EMAIL", user.getEmail());

		modelAndView.setViewName("user_add_email");
		return modelAndView;
	}

	@PostMapping("/addEmail")
	public String addEmail(Authentication authentication, @RequestBody Map<String, String> attributeMap,
			HttpServletRequest req) {
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return "LOGIN_FIRST";
		}

		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		User user = userService.getUser(username);
		if (StringUtils.hasText(user.getEmail()))
			return "EMAIL_ADDED_ALREADY";
		String email = attributeMap.get("email");
		String str = userService.validateEmail(email);
		if (str.equalsIgnoreCase("EXISTS"))
			return str;

		userService.sendVerificationEmail(user, email, Utility.getHostName(req));
		return "success";
	}

	@GetMapping("/verify/{code}")
	public ModelAndView verifyUser(@PathVariable String code) {
		ModelAndView modelAndView = new ModelAndView();
		if (userService.verify(code)) {
			modelAndView.setViewName("verify_success");
		} else {
			modelAndView.setViewName("verify_fail");
		}

		return modelAndView;
	}

	@GetMapping("/resendVerificationEmail")
	public ModelAndView showResendMailPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("resend_verification_email");
		return modelAndView;
	}

	@PostMapping("/resendVerificationEmail")
	public String resendVerificationEmail(@RequestBody Map<String, String> attributeMap) {
		String username = attributeMap.get("username");
		return userService.resendVerificationEmail(username);
	}

	@GetMapping("/sendPasswordResetLink")
	public ModelAndView sendPasswordResetLinkPage(Authentication authentication) {
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("user_forgot_password");
		} else {
			return new ModelAndView("redirect:/");
		}
	}

	@PostMapping("/sendPasswordResetLink")
	public String sendPasswordResetLink(Authentication authentication, @RequestBody Map<String, String> attributeMap,
			HttpServletRequest req) {
		String email = attributeMap.get("email");

		return userService.sendPasswordResetEmail(email, Utility.getHostName(req));
	}

	@GetMapping("/resetPassword/{code}")
	public ModelAndView resetPasswordPage(@PathVariable String code) {
		ModelAndView modelAndView = new ModelAndView("user_forgot_password");
		if (userService.verifyResetToken(code)) {
			modelAndView.addObject("RESET", "valid");
			modelAndView.addObject("token", code);
		} else {
			modelAndView.addObject("RESET", "invalid_token");
		}

		return modelAndView;
	}

	@PostMapping("/resetPassword")
	public String resetPassword(@RequestBody Map<String, String> attributeMap, Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {

		} else {
			String newPassword = attributeMap.get("password");
			String token = attributeMap.get("token");
			return userService.resetPassword(newPassword, token);
		}

		return null;
	}

	@GetMapping("/otpHistory")
	public ModelAndView getOtpHistory(Authentication authentication, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication == null || (authentication instanceof AnonymousAuthenticationToken)) {
			return new ModelAndView("redirect:/");
		}

		int currentPage = page.orElse(1);
		int pageSize = size.orElse(10);
		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		Page<UsersOtpHistory> otpHistoryPage = otpHistoryService
				.findPaginatedOtpHistory(PageRequest.of(currentPage - 1, pageSize), username);

		modelAndView.addObject("currentPage", currentPage);
		modelAndView.addObject("otpHistoryPage", otpHistoryPage);
		int totalPages = otpHistoryPage.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			modelAndView.addObject("pageNumbers", pageNumbers);
		}
		modelAndView.addObject("username", username);
		modelAndView.setViewName("user/users_otp_history");
		return modelAndView;
	}

	/******** GET BY ID *********/
	@GetMapping("/get/{id}")
	public ResponseEntity<?> getUserById(@PathVariable Long id) {
		return new ResponseEntity<>(userService.getProjectedUser(id), HttpStatus.OK);
	}

	/******** GET BY USERNAME *********/
	@GetMapping("/get/username/{username}")
	public ResponseEntity<?> getUserById(@PathVariable String username) {
		return new ResponseEntity<>(userService.getProjectedUserByUsername(username), HttpStatus.OK);
	}

	@GetMapping("/balance")
	public Double getBalance(Authentication authentication) {
		Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
		return userService.getBalance(id);
	}

	@GetMapping("/changePassword")
	public ModelAndView changePassword(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			User user = userService.getProjectedUser(id);
			modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		}

		modelAndView.setViewName("user/user_change_password");
		return modelAndView;
	}

	@PostMapping("/changePassword")
	public String changePassword(@RequestBody Map<String, String> attributeMap, Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			String currentPassword = attributeMap.get("currentPassword");
			String newPassword = attributeMap.get("password");
			return userService.changePassword(currentPassword, newPassword, id);
		}

		return null;
	}

	@GetMapping("/profile")
	public ModelAndView showProfile(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			User user = userService.getProjectedUser(id);
			modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		}

		modelAndView.setViewName("user/user_profile");
		return modelAndView;
	}

	@GetMapping("/updateEmail")
	public ModelAndView updateEmailPage(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			User user = userService.getProjectedUser(id);
			modelAndView.addObject("user", UserUtility.getUserWithNonSensitiveData(user));
		}

		modelAndView.setViewName("user/user_change_email");
		return modelAndView;
	}

}
