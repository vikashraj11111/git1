package com.rk.otp.controller;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.rk.app.persistence.entity.Announcement;
import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.MetaData;
import com.rk.app.persistence.entity.ServerCostPerc;
import com.rk.app.persistence.entity.ServerKeys;
import com.rk.app.persistence.entity.Services;
import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.entity.UsersOtpHistory;
import com.rk.app.persistence.entity.projection.ProfitEntity;
import com.rk.app.persistence.repository.MetaDataRepo;
import com.rk.app.persistence.repository.ServiceListRepository;
import com.rk.app.user.UserDetailsImpl;
import com.rk.app.utility.Utility;
import com.rk.otp.recharge.entity.OrderDetails;
import com.rk.otp.recharge.service.RechargeService;
import com.rk.otp.service.AdminService;
import com.rk.otp.service.AnnouncementService;
import com.rk.otp.service.CodeNumberArchiveService;
import com.rk.otp.service.CodeNumberService;
import com.rk.otp.service.OtpService;
import com.rk.otp.service.ServerCostPercService;
import com.rk.otp.service.ServerKeysService;
import com.rk.otp.service.ServicesService;
import com.rk.otp.service.UserBalanceAuditService;
import com.rk.otp.service.UserService;
import com.rk.otp.service.UsersOtpHistoryService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/admin")
public class AdminController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

	@Autowired
	private UserService userService;

	@Autowired
	private ServicesService servicesService;

	@Autowired
	private ServiceListRepository serviceListRepository;

	@Autowired
	private UserBalanceAuditService userBalanceAuditService;

	@Autowired
	private OtpService otpService;

	@Autowired
	private ServerKeysService serverKeysService;

	@Autowired
	private AnnouncementService announcementService;

	@Autowired
	private CodeNumberService codeNumberService;

	@Autowired
	private UsersOtpHistoryService otpHistoryService;

	@Autowired
	private AdminService adminService;

	@Autowired
	private ServerCostPercService serverCostPercService;

	@Autowired
	private MetaDataRepo metaDataRepo;

	@Autowired
	private CodeNumberArchiveService codeNumberArchiveService;

	@Autowired
	private RechargeService rechargeService;

	@Value("${LOG_DIR}")
	private String logsDir;

	@GetMapping(value = { "/console", "/" })
	public ModelAndView getUserHome(Authentication authentication) {
		ModelAndView modelAndView = new ModelAndView();
		String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		modelAndView.addObject("username", username);
		modelAndView.setViewName("admin/admin_console");
		return modelAndView;
	}

	@GetMapping("/updateUser")
	public ModelAndView showAddBalanceToUserPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_add_balance_user");
		return modelAndView;
	}

	@PutMapping(value = "/updateUser")
	public String addUserBalance(@RequestBody User user, Authentication authentication) {
		String byUser = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		String username = user.getUsername();
		double amount = user.getBalance();
		if (amount > 3000)
			return "HIGH_AMOUNT";
		user = userService.addBalance(username, amount, byUser, null);
		if (user != null) {
			return String.valueOf(user.getBalance());
		}
		return "NOT_EXIST";
	}

	@GetMapping("/listUsers")
	public ModelAndView listAllUsers(@RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size) {
		ModelAndView modelAndView = new ModelAndView();
		int currentPage = page.orElse(1);
		int pageSize = size.orElse(10);
		Page<User> userPage = userService.findPaginatedUser(PageRequest.of(currentPage - 1, pageSize),
				!page.isPresent());

		modelAndView.addObject("currentPage", currentPage);
		modelAndView.addObject("userPage", userPage);
		int totalPages = userPage.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			modelAndView.addObject("pageNumbers", pageNumbers);
		}

		modelAndView.setViewName("admin/admin_users_list");
		return modelAndView;
	}

	@GetMapping("/checkBalanceUser")
	public ModelAndView showUserBalancePage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_users_balance");
		return modelAndView;
	}

	@GetMapping("/checkBalanceUser/{username}")
	public String checkUserBalance(@PathVariable String username) {
		Optional<User> user = userService.getProjectedUserByUsername(username);

		if (user.isEmpty())
			return "User Does not exist";
		else {
			return String.valueOf(user.get().getBalance());
		}
	}

	@GetMapping("/deductUserBalance")
	public ModelAndView showDeductUserBalancePage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_deduct_balance_user");
		return modelAndView;
	}

	@PutMapping("/deductUserBalance")
	public String deductUserBalance(@RequestBody User user, Authentication authentication) {
		String byUser = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
		String username = user.getUsername();
		double amount = user.getBalance();
		user = userService.deductBalance(username, amount, byUser, null);
		if (user != null) {
			return String.valueOf(user.getBalance());
		}
		return "NOT_EXIST";
	}

	@GetMapping("/listServices")
	public ModelAndView showServicesPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.setViewName("admin/admin_services");
		return modelAndView;
	}

	@GetMapping("/listServices/{server}")
	public ModelAndView getServiceList(@PathVariable String server, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size) {

		ModelAndView modelAndView = new ModelAndView();
		int currentPage = page.orElse(1);
		int pageSize = size.orElse(10);

		Page<Services> servicesPage = servicesService.findPaginatedService(PageRequest.of(currentPage - 1, pageSize),
				!page.isPresent(), server);

		modelAndView.addObject("currentPage", currentPage);
		modelAndView.addObject("servicesPage", servicesPage);
		int totalPages = servicesPage.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			modelAndView.addObject("pageNumbers", pageNumbers);
		}
		modelAndView.addObject("server", server);
		modelAndView.setViewName("admin/admin_get_service_list");

		return modelAndView;
	}

	@GetMapping("/priceList")
	public ModelAndView showPriceListServers() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.setViewName("admin/admin_price_list_select_server");
		return modelAndView;
	}

	@GetMapping("/priceList/{server}")
	public ModelAndView getPriceListFromServer(@PathVariable String server) {
		ModelAndView modelAndView = new ModelAndView();
		Map<String, List<String>> servicePriceMap = new TreeMap<>();

//		TODO: call api to specified server and get pricelist as json
		String result = otpService.getPriceAll(server);
		// TODO: fetch services based on server
		List<Services> allServices = servicesService.getAllServices();
		if (result != null) {
			try {
				JSONObject jsonObject = new JSONObject(result).getJSONObject("22");
				jsonObject.keys().forEachRemaining(key -> {
					String cost = jsonObject.getJSONObject(key).getNumber("cost").toString();
					String count = jsonObject.getJSONObject(key).get("count").toString();
					allServices.stream().anyMatch(service -> {
						if (service.getServiceCode().equalsIgnoreCase(key)) {
							servicePriceMap.put(service.getServiceName(), Arrays.asList(cost, count));
							return true;
						}
						servicePriceMap.put(key, Arrays.asList(cost, count));
						return false;
					});
				});
			} catch (JSONException e) {
				LOGGER.error("JsonException occurred : ", e);
			}
		}

		modelAndView.addObject("priceList", servicePriceMap);
		modelAndView.setViewName("admin/admin_price_list");
		return modelAndView;
	}

	@GetMapping("/getPriceFromServer/{server}/{serviceId}")
	public Double getPriceFromServer(@PathVariable String server, @PathVariable String serviceId) {
		return otpService.getPriceFromServer(serviceId, server);
	}

	@GetMapping("/usersCombinedBalance")
	public String getUsersCombinedBalance() {
		return userService.sumOfUsersBalance();
	}

	@GetMapping("/activeUsersBalance")
	public String getActiveUsersBalance() {
		return userService.getActiveUsersBalance();
	}

	@GetMapping("/changePassword")
	public ModelAndView showChangePasswordPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_change_password");
		return modelAndView;
	}

	@PostMapping("/changePassword")
	public boolean changePassword(@RequestBody User user) {
		return userService.changePassword(user);
	}

	@GetMapping("/summary")
	public ModelAndView showSummaryPage() {
		ModelAndView modelAndView = new ModelAndView();

		modelAndView.addObject("todaysAddedBalance", userBalanceAuditService.findTodaysAddedBalance());
		modelAndView.addObject("profits", codeNumberArchiveService.getTodaysProfit());
		modelAndView.setViewName("admin/admin_summary");
		return modelAndView;
	}

	@GetMapping("/balance/{server}")
	public String getBalance(@PathVariable String server, Authentication authentication) {

		return otpService.getBalance(server);
	}

	@GetMapping("/updateKey")
	public ModelAndView showUpdateKeyPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.setViewName("admin/admin_update_server_key");
		return modelAndView;
	}

	@GetMapping("/getKey/{server}")
	public String getKey(@PathVariable String server) {

		return serverKeysService.getServerKey(server);
	}

	@PostMapping("/updateKey")
	public String updateKey(@RequestBody Map<String, String> attributeMap) {
		String updatedKey = attributeMap.get("updatedKey");
		String server = attributeMap.get("server");

		if (!(StringUtils.hasText(updatedKey) && StringUtils.hasText(server)))
			return "ERROR";

		updatedKey = updatedKey.trim();
		if (serverKeysService.update(server, updatedKey))
			return "UPDATED";

		return null;
	}

	@PostMapping("/verifyKey")
	public boolean verifyKey(@RequestBody Map<String, String> attributeMap) {
		String updatedKey = attributeMap.get("updatedKey");
		String server = attributeMap.get("server");
		String verifyKey = otpService.verifyKey(updatedKey, server);

		return !"BAD_KEY".equalsIgnoreCase(verifyKey);
	}

	@GetMapping("/announcement")
	public ModelAndView showAnnouncementPage() {
		ModelAndView modelAndView = new ModelAndView();
		List<Announcement> announcements = announcementService.getAnnouncements();
		modelAndView.addObject("announcements", announcements);
		modelAndView.setViewName("admin/admin_announcements");
		return modelAndView;
	}

	@PostMapping("/addAnnouncement")
	public String addAnnouncement(@RequestBody Announcement announcement, Authentication authentication) {
		announcement.setUser(((UserDetailsImpl) authentication.getPrincipal()).getUsername());
		Long id = announcementService.addAnnouncement(announcement);
		if (id != null)
			return String.valueOf(id);

		return "ERROR";
	}

	@PostMapping("/deleteAnnouncement")
	public String deleteAnnouncement(@RequestBody Map<String, String> attributeMap) {
		if (announcementService.deleteAnnouncement(Long.parseLong(attributeMap.get("id"))))
			return "Deleted";

		return "ERROR";
	}

	@PostMapping("/updateServicePrice")
	public boolean updateServicePrice(@RequestBody Map<String, String> attributeMap) {
		String service = attributeMap.get("service");
		String price = attributeMap.get("price");
		Optional<Services> optionalService = servicesService.findById(service);
		if (optionalService.isPresent()) {
			return servicesService.updateServicePrice(Double.parseDouble(price), service);
		}
		return false;
	}

	@PostMapping("/searchUser")
	public ResponseEntity<?> searchUser(@RequestBody Map<String, String> attributeMap) {
		String keyword = attributeMap.get("keyword");
		List<User> searchResult = userService.searchUserByUsernameKeyword(keyword);

		return ResponseEntity.ok(searchResult);
	}

	@GetMapping("/manualRecharge")
	public ModelAndView showManualRechargePage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_manual_recharge");
		return modelAndView;
	}

//	@PostMapping("/manualRecharge")
//	public ResponseEntity<?> doManualRecharge(@RequestBody Map<String, String> attributeMap) {
//		String utr = attributeMap.get("utr");
//		String username = attributeMap.get("username");
//		String paymentMode = attributeMap.get("paymentMode");
//		if (userService.getUser(username) == null)
//			return ResponseEntity.ok("NOT_EXIST");
//
//		if ("true".equalsIgnoreCase(restCallService.checkRechargeStatus(utr)))
//			return ResponseEntity.ok("ALREADY_USED");
//
//		String result = "";
//
//		if ("paytm".equalsIgnoreCase(paymentMode)) {
//			result = userService.getRechargeStatus(utr, username, paymentMode, "true");
//			return ResponseEntity.ok(result);
//		} else {
//			result = restCallService.doManualRecharge(utr);
//		}
//
//		if (!(result == null || "ALREADY_USED".equalsIgnoreCase(result) || "FAILED".equalsIgnoreCase(result)
//				|| "MORE_THAN_ONE_DAY".equalsIgnoreCase(result) || "NOT_FOUND".equalsIgnoreCase(result)
//				|| "BEFORE_16_JULY".equalsIgnoreCase(result) || "ERROR".equalsIgnoreCase(result)
//				|| "CURRENCY_NOT_INR".equalsIgnoreCase(result) || "BEFORE_19_OCT".equalsIgnoreCase(result)
//				|| "CONTACT_ADMIN".equalsIgnoreCase(result))) {
//			double rechargeValue = Double.parseDouble(result);
//			User user = userService.addBalance(username, rechargeValue, "RECHARGE", null);
//			return ResponseEntity.ok(user.getBalance());
//		}
//
//		if (result == null)
//			return ResponseEntity.ok("ERROR");
//		else
//			return ResponseEntity.ok(result);
//	}

	@GetMapping("/activeNumbers")
	public ModelAndView showActiveNumbersPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("activeNumbers", codeNumberService.getAllCodeNumbers());
		modelAndView.setViewName("admin/admin_active_numbers_list");
		return modelAndView;
	}

	@GetMapping("/otpHistory/{username}")
	public ModelAndView getOtpHistory(@PathVariable String username, @RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size) {
		ModelAndView modelAndView = new ModelAndView();
		int currentPage = page.orElse(1);
		int pageSize = size.orElse(10);
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
		modelAndView.setViewName("admin/admin_users_otp_history");

		return modelAndView;
	}

	@GetMapping("/addService")
	public ModelAndView showAddServicePage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.addObject("serviceList", serviceListRepository.findAllByOrderByServiceName());
		modelAndView.setViewName("admin/admin_add_service");
		return modelAndView;
	}

	@GetMapping("/getServiceListPage")
	public ModelAndView showServiceListPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("serviceList", serviceListRepository.findAllByOrderByServiceName());
		modelAndView.setViewName("admin/admin_services_list");
		return modelAndView;
	}

	@PostMapping("/addService")
	public String addService(@RequestBody Map<String, String> attributeMap) {
		String serviceName = attributeMap.get("serviceName");
		String serviceCode = attributeMap.get("serviceCode");
		String server = attributeMap.get("server");
		Double price = Double.parseDouble(attributeMap.get("price"));
		String serviceId = attributeMap.get("serviceId");
		serviceName = Utility.removeUnwantedChars(serviceName);

		String code = serviceName.toLowerCase().concat(server.substring(server.length() - 1));

		Optional<String> servicecode2 = servicesService.findServiceCodeByCode(code);
		if (servicecode2.isPresent())
			return "ALREADY_PRESENT";

		if (servicesService.addNewService(server, serviceCode, serviceName, code, price, serviceId))
			return "SUCCESS";

		return "FAILURE";
	}

//	@GetMapping("/copyServices/{fromServer}/{toServer}")
//	public String copyServices(@PathVariable String fromServer, @PathVariable String toServer) {
//		System.out.println("fromServer : " + fromServer + " ::: toServer : " + toServer);
//		if(fromServer.equals("server3")) {
//			if(toServer.equals("server1")) {
//				return servicesService.copyServices(fromServer, toServer);
//			}
//		}
//		
//		return "NOT DONE";
//	}

	@GetMapping("/comparePrice")
	public ModelAndView showPriceCompareServerSelectionPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.setViewName("admin/admin_price_compare_select_server");
		return modelAndView;
	}

	@GetMapping("/comparePrice/{server}")
	public ModelAndView getActualPricePage(@PathVariable String server) {
		ModelAndView modelAndView = new ModelAndView();
		double percentage = serverCostPercService.getPercentage(server);
		Map<String, List<Double>> servicePriceMap = adminService.getActualCostOfServices(server, percentage);
		modelAndView.addObject("priceList", servicePriceMap);
		modelAndView.addObject("percentage", percentage);
		modelAndView.setViewName("admin/admin_price_compare");
		return modelAndView;
	}

	@GetMapping("/comparePriceAllServers")
	public ModelAndView comparePrices() {
		ModelAndView modelAndView = new ModelAndView();
		List<String> serversList = new ArrayList<>(serverKeysService.findAllServers());
		serversList.remove("server6");
//		serversList.remove("server2");
		serversList.remove("server3");
		serversList.remove("server4");
		modelAndView.addObject("serverActualPriceMap", adminService.comparePriceAllServers(serversList));
		modelAndView.addObject("serversList", serversList);
		modelAndView.setViewName("admin/admin_price_compare_all");
		return modelAndView;
	}

	@GetMapping("/setServerPercentage")
	public ModelAndView getServerPercentagePage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("servers", serverKeysService.findAllServers());
		modelAndView.setViewName("admin/admin_set_server_percentage");
		return modelAndView;
	}

	@GetMapping("/getServerPercentage/{server}")
	public String getServerPercentage(@PathVariable String server) {
		ServerCostPerc serverCostPerc = serverCostPercService.getById(server);

		return serverCostPerc.getPerc() + ":" + serverCostPerc.getActualCost();
	}

	@PostMapping("/setServerPercentage")
	public String setServerPercentage(@RequestBody Map<String, String> attributeMap) {
		ServerCostPerc serverCostPerc = null;
		try {
			String server = attributeMap.get("server");
			Double actualCostPer10k = Double.parseDouble(attributeMap.get("actualCostPer10k"));
			if (actualCostPer10k > 10000)
				return "ERROR";
			serverCostPerc = serverCostPercService.saveAndFlush(server, (10000 / actualCostPer10k) * 100,
					actualCostPer10k);
		} catch (Exception e) {
			return "ERROR";
		}

		return String.valueOf(serverCostPerc.getPerc());
	}

	@GetMapping("/serverTools")
	public ModelAndView getServerToolsPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_server_tools");
		return modelAndView;
	}

	@GetMapping("/userTools")
	public ModelAndView getUserToolsPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_user_tools");
		return modelAndView;
	}

	@GetMapping("/createBackup")
	public ModelAndView createBackupPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_create_backup");
		return modelAndView;
	}

	@PostMapping("/createBackup")
	public String createBackup(@RequestBody Map<String, String> attributeMap) throws IOException {

		return ServletUriComponentsBuilder.fromCurrentContextPath().path("/file/downloadFile/")
				.path(adminService.backupFile(attributeMap.get("service"))).toUriString();
	}

	@GetMapping("/restoreBackup")
	public ModelAndView restoreBackupPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("isEnabled", metaDataRepo.findValueByAttribute("isRestoreEnabled"));
		modelAndView.setViewName("admin/admin_restore_backup");
		return modelAndView;
	}

	@PostMapping("/restoreBackup")
	public ResponseEntity<?> restoreBackup(@RequestParam MultipartFile file) {

		return adminService.restoreBackup(file);
	}

	@GetMapping("/rechargeHistory/{username}")
	public ModelAndView getRechargeHistory(@PathVariable String username) {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("rechargeHistoryList", userBalanceAuditService.findRechargeHistory(username));
		modelAndView.addObject("username", username);
		modelAndView.setViewName("admin/admin_users_recharge_history");

		return modelAndView;
	}

	@GetMapping("/getUsernameByUtr")
	public ModelAndView getUsernameByUtrPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("admin/admin_get_username_by_utr");
		return modelAndView;
	}

	@PostMapping("/getOrderByUtr")
	public ResponseEntity<OrderDetails> getOrderByUtr(@RequestBody Map<String, String> attributeMap) {
		String utr = attributeMap.get("utr");

		return ResponseEntity.ok(rechargeService.getOrderDetailsByUtr(utr));
	}

	@GetMapping("/getAllOrders")
	public ModelAndView getAllOrders(@RequestParam("page") Optional<Integer> page,
			@RequestParam("size") Optional<Integer> size) {
		ModelAndView modelAndView = new ModelAndView();
		int currentPage = page.orElse(1);
		int pageSize = size.orElse(10);
		Page<OrderDetails> orderDetailsPage = rechargeService
				.getPaginatedOrders(PageRequest.of(currentPage - 1, pageSize));
		modelAndView.addObject("currentPage", currentPage);
		modelAndView.addObject("orderDetails", orderDetailsPage);
		int totalPages = orderDetailsPage.getTotalPages();
		if (totalPages > 0) {
			List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
			modelAndView.addObject("pageNumbers", pageNumbers);
		}
		modelAndView.setViewName("admin/admin_get_all_orders");
		return modelAndView;
	}

	@PostMapping("/searchOrders")
	public ResponseEntity<List<OrderDetails>> searchOrders(@RequestBody Map<String, String> attributeMap) {
		String keyword = attributeMap.get("keyword");
		List<OrderDetails> searchResult = rechargeService.searchOrdersByKeyword(keyword);

		return ResponseEntity.ok(searchResult);
	}

	@GetMapping("/serverList")
	public ModelAndView getServerList() {
		ModelAndView modelAndView = new ModelAndView();
		List<ServerKeys> serverKeys = serverKeysService.findAll();
		Collections.sort(serverKeys, Comparator.comparing(ServerKeys::getServer));
		modelAndView.addObject("serverKeysList", serverKeys);
		modelAndView.setViewName("admin/admin_server_list");
		return modelAndView;
	}

	@PostMapping("/disableServer")
	public ResponseEntity<?> disableServer(@RequestBody Map<String, String> attributeMap) {
		String server = attributeMap.get("server");
		boolean result = serverKeysService.disableServer(server);
		return ResponseEntity.ok(result ? "success" : "failure");
	}

	@PostMapping("/enableServer")
	public ResponseEntity<?> enableServer(@RequestBody Map<String, String> attributeMap) {
		String server = attributeMap.get("server");
		boolean result = serverKeysService.enableServer(server);
		return ResponseEntity.ok(result ? "success" : "failure");
	}

	@GetMapping("/showOtp/{code}")
	public ModelAndView showOtpPageByService(Authentication authentication, @PathVariable String code) {
		ModelAndView modelAndView = new ModelAndView();
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {

			CodeNumber codeNumber = codeNumberService.findById(code).orElse(null);
			if (codeNumber == null) {
				modelAndView.addObject("error", "code not found");
			} else {
				modelAndView.addObject("codeNumber", codeNumber);
			}
		}
		modelAndView.setViewName("admin/admin_otp_service");
		return modelAndView;
	}

	@GetMapping("/updateMetadata")
	public ModelAndView updateMetadataPage() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("metadatas", metaDataRepo.findAll());
		modelAndView.setViewName("admin/admin_update_meta_data");
		return modelAndView;
	}

	@PostMapping("/updateMetaData")
	public String updateMetaData(@RequestBody Map<String, String> attributeMap) {
		String updatedValue = attributeMap.get("updatedValue");
		String attribute = attributeMap.get("attribute");

		if (!(StringUtils.hasText(updatedValue) && StringUtils.hasText(attribute)))
			return "ERROR";

		updatedValue = updatedValue.trim();
		List<MetaData> metaDataList = metaDataRepo.findByAttribute(attribute);

		if (metaDataList.isEmpty())
			return null;

		MetaData metaData = metaDataList.get(0);
		metaData.setValue(updatedValue);
		metaDataRepo.saveAndFlush(metaData);
		return "UPDATED";
	}

	@GetMapping("/getLogs")
	public ModelAndView getLogs() {
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.addObject("fileNames", Utility.listFilesUsingJavaIO(logsDir).stream()
				.filter(f -> f.endsWith(".log") || f.endsWith(".html")).collect(Collectors.toSet()));
		modelAndView.setViewName("admin/admin_download_logs");
		return modelAndView;
	}

	@GetMapping("/downloadLog/{fileName:.+}")
	public ResponseEntity<Resource> downloadLogFile(@PathVariable String fileName, HttpServletRequest request) {
		// Load file as Resource
		try {
			Path filePath = Paths.get(logsDir).toAbsolutePath().normalize().resolve(fileName).normalize();
			Resource resource = new UrlResource(filePath.toUri());

			String contentType = null;
			try {
				contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
			} catch (IOException ex) {
				LOGGER.info("Could not determine file type.");
			}

			// Fallback to the default content type if type could not be determined
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (MalformedURLException e) {
			LOGGER.error("Error in getLogs :: fileName :: {} ", fileName, e);
		}

		return ResponseEntity.notFound().build();
	}

	@GetMapping("/viewLog/{fileName:.+}")
	public ResponseEntity<Resource> viewLog(@PathVariable String fileName, HttpServletRequest request) {
		// Load file as Resource
		try {
			Path filePath = Paths.get(logsDir).toAbsolutePath().normalize().resolve(fileName).normalize();
			Resource resource = new UrlResource(filePath.toUri());

			String contentType = null;
			try {
				contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
			} catch (IOException ex) {
				LOGGER.info("Could not determine file type.");
			}

			// Fallback to the default content type if type could not be determined
			if (contentType == null) {
				contentType = "application/octet-stream";
			}

			return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
					.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
					.body(resource);
		} catch (MalformedURLException e) {
			LOGGER.error("Error in getLogs :: fileName :: {} ", fileName, e);
		}

		return ResponseEntity.notFound().build();
	}

	@GetMapping("/getProfitOverview")
	public ModelAndView getProfitOverview() {
		ModelAndView modelAndView = new ModelAndView();
		List<ProfitEntity> last30DaysProfit = codeNumberArchiveService.getLast30DaysProfit();
		final Map<String, Set<ProfitEntity>> updatedMap = last30DaysProfit.stream().collect(Collectors.groupingBy(
				p -> p.getArchiveTime(), LinkedHashMap::new, Collectors.mapping(p -> p, Collectors.toSet())));
		modelAndView.addObject("profitsByDate", updatedMap);
		modelAndView.setViewName("admin/admin_profit_overview");

		return modelAndView;
	}

}
