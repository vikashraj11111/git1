package com.rk.otp.service.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.CodeNumberArchive;
import com.rk.app.persistence.entity.UsersOtpHistory;
import com.rk.app.utility.Utility;
import com.rk.otp.properties.Properties;
import com.rk.otp.servers.ConstanskyServer;
import com.rk.otp.servers.FastSmsServer;
import com.rk.otp.servers.GrizzlySmsServer;
import com.rk.otp.servers.ServerApi;
import com.rk.otp.servers.SmsActivateServer;
import com.rk.otp.servers.SmsBowerServer;
import com.rk.otp.servers.TempNumServer;
import com.rk.otp.service.CodeNumberArchiveService;
import com.rk.otp.service.CodeNumberService;
import com.rk.otp.service.MailSentFlagService;
import com.rk.otp.service.OtpService;
import com.rk.otp.service.ServerBalanceService;
import com.rk.otp.service.ServerCostPercService;
import com.rk.otp.service.ServerKeysService;
import com.rk.otp.service.ServicesService;
import com.rk.otp.service.UserService;
import com.rk.otp.service.UsersOtpHistoryService;
import com.rk.otp.validation.service.CheckRegistrationStatusService;

@Service
public class OtpServiceImpl implements OtpService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OtpServiceImpl.class);

	@Autowired
	private CodeNumberService codeNumberService;

	@Autowired
	private UserService userService;

	@Autowired
	private ServicesService servicesService;

	@Autowired
	private UsersOtpHistoryService usersOtpHistoryService;

	@Autowired
	private Properties properties;

	@Autowired
	private MailSentFlagService mailSentFlagService;

	@Autowired
	private CustomMailSender mailSender;

	@Autowired
	private ServerKeysService serverKeysService;

	@Autowired
	private CheckRegistrationStatusService checkRegistrationStatusService;

	@Autowired
	private ServerBalanceService serverBalanceService;

	@Autowired
	private ServerCostPercService serverCostPercService;

	@Autowired
	private CodeNumberArchiveService archiveService;

	@Autowired
	private ApplicationContext context;

	@Override
	public String getCodeNumber(Double amount, String server, String service, String username, int retry) {
		if (retry >= 5) {
			return "ERROR";
		}

		String serviceId = servicesService.findServiceCodeByCode(service).get();
		ServerApi serverApi = getServer(server);
		Double priceFromServer = getPriceFromServer(serviceId, server);
		if (priceFromServer == null) {
			sendEmail("Unable to get price of this service -> " + service + " (" + serviceId + ").\n"
					+ "Please validate the price from server.\n\n"
					+ "Setting the price from server to 5.5 to bypass this.", server, "SYSTEM", service);
			priceFromServer = 5.5;
		}

		double percentage = serverCostPercService.getPercentage(server);
		Double actualPriceFromServer = priceFromServer * percentage * 0.01;

		if (actualPriceFromServer > 0 && actualPriceFromServer + 2 > amount) {

			sendEmail("Increase service price: " + service + "\n\n" + "Actual Price of service: "
					+ actualPriceFromServer + "\n" + "Website Service price: " + amount, server, username, service);

			return "Some Error has occurred. Contact Admin";
		}

		String result = serverApi.getNumber(serverKeysService.getServerKey(server), serviceId);

		if ("BAD_KEY".equalsIgnoreCase(result)) {
			sendEmail(result, server, username, service);
			return "ERROR";
		}
		if ("NO_BALANCE".equalsIgnoreCase(result)) {
			sendEmail(result, server, username, service);
			return "ERROR";
		}

		removeMailSentFlag(server);

		if ("ERROR".equalsIgnoreCase(result) || "NO_NUMBER".equalsIgnoreCase(result))
			return result;

		String[] arr = result.split(":");
		String code = arr[1];
		String number = arr[0];

		if (server.equalsIgnoreCase("server6")) {
			String balance = arr[2];
			serverBalanceService.saveServerBalance(server, balance);
		}

		if (service.toLowerCase().contains("dream11new")) {
			String registrationStatus = checkRegistrationStatusService.checkDream11Status(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("true")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("dream11old")) {
			String registrationStatus = checkRegistrationStatusService.checkDream11Status(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("false")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("flipkartnew")) {
			String registrationStatus = checkRegistrationStatusService.checkFlipkartStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("true")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("flipkartold")) {
			String registrationStatus = checkRegistrationStatusService.checkFlipkartStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("false")) {
				LOGGER.info("Cancelling number :: {}", number);
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("swiggynew")) {
			String registrationStatus = checkRegistrationStatusService.checkSwiggyStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("true")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("swiggyold")) {
			String registrationStatus = checkRegistrationStatusService.checkSwiggyStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("false")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("paytmnew")) {
			String registrationStatus = checkRegistrationStatusService.checkPaytmStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("true")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		if (service.toLowerCase().contains("paytmold")) {
			String registrationStatus = checkRegistrationStatusService.checkPaytmStatus(number).getBody().toString();
			if (registrationStatus.equalsIgnoreCase("false")) {
				cancelRegisteredNumber(code, server); // cancelling registered number
				return getCodeNumber(amount, server, service, username, ++retry);
			}
		}

		// Insert into CodeNumber
		CodeNumber codeNumber = new CodeNumber();
		codeNumber.setCode(code);
		codeNumber.setNumber(number);
		codeNumber.setPrice(amount);
		codeNumber.setService(service);
		codeNumber.setCreatedTime(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
		codeNumber.setServer(server);
		codeNumber.setActualPrice(actualPriceFromServer);

		codeNumber.setUser(username);

		codeNumberService.saveAndFlush(codeNumber);

		if (userService.deductBalance(username, amount, "SYSTEM", code) != null) {
			codeNumber.setBalanceDeducted("Y");
			codeNumberService.saveAndFlush(codeNumber);
		}

		return result;
	}

	private ServerApi getServer(String server) {
		ServerApi serverApi = context.getBean(TempNumServer.class);
		if (server.equals("server2"))
			serverApi = context.getBean(SmsActivateServer.class);
		else if (server.equals("server3"))
			serverApi = context.getBean(SmsBowerServer.class);
		else if (server.equals("server4"))
			serverApi = context.getBean(FastSmsServer.class);
		else if (server.equals("server5"))
			serverApi = context.getBean(GrizzlySmsServer.class);
		else if (server.equals("server6"))
			serverApi = context.getBean(ConstanskyServer.class);

		return serverApi;
	}

	private void cancelRegisteredNumber(String code, String server) {
		getServer(server).cancel(serverKeysService.getServerKey(server), code);

	}

	@Override
	public String getOtpByCode(String service, String server, String code, String username, CodeNumber codeNumber,
			boolean isAdmin) {

		String result = getServer(server).getOtp(serverKeysService.getServerKey(server), code);

		if (result.equals("ERROR") || result.equals("BAD_KEY") || result.equals("ERROR_OCCURRED")) {
			sendEmail(result, server, username, service);
			return "ERROR";
		}

		if (result.equals("NO_ACTIVATION") || result.equals("CANCEL") || result.equals("STATUS_CANCEL"))
			return "CANCEL";

		List<UsersOtpHistory> otpHistoryByCode = usersOtpHistoryService.getOtpHistoryByCode(code);

		if ((result.equals("WAITING") || result.equals("STATUS_WAIT_CODE"))
				|| result.equals("Api_blocked_for_5_seconds") && otpHistoryByCode.isEmpty())
			return "WAITING";
		else if ((result.equals("WAITING") || result.equals("STATUS_WAIT_CODE"))
				|| result.equals("Api_blocked_for_5_seconds") && !otpHistoryByCode.isEmpty())
			return otpHistoryByCode.get(0).getReceivedOtp();

		removeMailSentFlag(server);

		try {
			UsersOtpHistory otpHistory;
			if (!otpHistoryByCode.isEmpty()) {
				otpHistory = otpHistoryByCode.get(0);
				otpHistory.setReceivedOtp(Utility.getMaxVarChar(result));
				usersOtpHistoryService.saveHistory(otpHistory);
			} else {
				// Adding otp in history
				if (service.contains("lazypay") && !isAdmin) {
					mailSender.sendLazyPayMail(codeNumber.getNumber(), code, result);
					TimeUnit.SECONDS.sleep(10);
				}
				otpHistory = new UsersOtpHistory(username, codeNumber.getNumber(), codeNumber.getCode(),
						Utility.getMaxVarChar(result), service);
				usersOtpHistoryService.saveHistory(otpHistory);

				codeNumber.setOtpReceived("Y");
				codeNumberService.saveAndFlush(codeNumber);

				userService.addOtpCount(username); // increment otp count
				LOGGER.debug("Otp received was :: {}", result);
			}

		} catch (Exception ex) {
			LOGGER.error("Exception while updating otpReceived to Y occurred");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			String sStackTrace = sw.toString();
			mailSender.sendEmailToAdminHtml("Error in balance deduction - " + username,
					"Otp: " + result + "<br>" + "Server : " + server + "<br>" + "Code : " + code + "<br>" + "Number: "
							+ codeNumber.getNumber() + "<br><br>" + "<pre>Exception occurred :: " + sStackTrace
							+ "</pre>");
			return "ERROR";
		}

		return result;
	}

	@Override
	public boolean cancelActivation(String code, Authentication authentication) {
		CodeNumber codeNumber = codeNumberService.getByCode(code);
		if (codeNumber == null)
			return false;

		String server = codeNumber.getServer();

		if ((server.equalsIgnoreCase("server2") || server.equalsIgnoreCase("server3")) && TimeUnit.MILLISECONDS
				.toSeconds(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))).getTime()
						- codeNumber.getCreatedTime().getTime()) < 120) {
			return false;
		}

		String result = cancelActivation(code, server);

		if ("N".equalsIgnoreCase(codeNumber.getOtpReceived()) && ("ACCESS_CANCEL".equalsIgnoreCase(result)
				|| ("BAD_STATUS".equalsIgnoreCase(result) && codeNumber.getOtpReceived().equalsIgnoreCase("N")))) {
			if (userService.refund(authentication, codeNumber.getPrice(), code)) {
				codeNumberService.deleteById(code);
				return true;
			}

			return false;
		} else if ("EARLY_CANCEL_DENIED".equalsIgnoreCase(result)) {
			System.out.println("Cancellation not allowed before two minutes!!!");
			return false;
		} else {
			codeNumber.setIsCancelled("Y");
			codeNumberService.saveAndFlush(codeNumber);
			return true;
		}

	}

	public String cancelActivation(String code, String server) {
		return getServer(server).cancel(serverKeysService.getServerKey(server), code);
	}

	@Override
	public String getPriceAll(String server) {
		String result = getServer(server).getPriceAll(serverKeysService.getServerKey(server));

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";

		return result;
	}

	@Override
	public String getBalance(String server) {
		if (server.equalsIgnoreCase("server6")) {
			return serverBalanceService.getServerBalance(server);
		}

		String result = getServer(server).getBalance(serverKeysService.getServerKey(server));

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "ERROR";

		return result;
	}

	@Override
	public boolean resend(String code, Authentication authentication) {
		CodeNumber codeNumber = codeNumberService.getByCode(code);
		String server = codeNumber.getServer();
		String result = getServer(server).resend(serverKeysService.getServerKey(server), code);

		return "Success".equalsIgnoreCase(result);
	}

	/********************************************************************************************
	 * @param java.lang.String service
	 * @param java.lang.String server
	 * @return java.lang.Double
	 ********************************************************************************************/

	@Override
	public Double getPriceFromServer(String service, String server) {
		String result = getServer(server).getPrice(serverKeysService.getServerKey(server), service);

		if ("BAD_KEY".equalsIgnoreCase(result))
			return 0.0;

		if (result == null) {
			return null;
		}

		return Double.parseDouble(result);
	}

	private void sendEmail(String msg, String server, String username, String service) {
		if (mailSentFlagService.getByServerAndError(server, msg) > -1)
			return;

		properties.putMailSentFlagInMap(server, true);
		mailSentFlagService.save(server, true, msg);

		if ("BAD_KEY".equalsIgnoreCase(msg))
			mailSender.sendEmail("BAD_KEY : Access Key changed", server
					+ " ka access Key change ho gya hai. Wapas add karo." + "\n\n" + "Reported by user: " + username);
		else if ("NO_BALANCE".equalsIgnoreCase(msg)) {
			mailSender.sendEmail("NO_BALANCE :: Recharge Immedietly",
					server + " ka Balance khatam ho gya hai. Recharge karo jaldi se." + "\n\n" + "Reported by user: "
							+ username);
		} else {
			StringBuilder message = new StringBuilder();
			message.append("Message: ").append(msg).append("\n\n").append("Username: ").append(username).append("\n\n")
					.append("Server: ").append(server);
			if (service != null)
				message.append("\n\n").append("Service: ").append(service);
			mailSender.sendEmail(message.toString());
		}

	}

	private void removeMailSentFlag(String server) {

		if (properties.getMailSentServerMap().get(server) != null && !mailSentFlagService.isMailSentForServer(server)) {
			properties.putMailSentFlagInMap(server, false);
			mailSentFlagService.deleteAllByServer(server);
		}
	}

	@Override
	public String verifyKey(String updatedKey, String server) {
		return getServer(server).getBalance(serverKeysService.getServerKey(server));
	}

	@Override
	public void refundUserIfOtpNotReceivedIn20Mins() {
		Optional<List<CodeNumber>> allAfter20MinNotUsed = codeNumberService.getAllAfter20MinNotUsed();
		refundUserIfNotUsed(allAfter20MinNotUsed);
	}

	@Override
	public void deleteCodeNumberAfter25Mins() {
		Optional<List<CodeNumber>> allAfter25Min = codeNumberService.getAllAfter25Min();
		if (allAfter25Min.isPresent() && !allAfter25Min.get().isEmpty()) {
			archiveAndDeleteCodeNumberList(allAfter25Min.get());
		}
	}

	@Override
	public void deleteUsedCancelled() {
		Optional<List<CodeNumber>> usedCancelled = codeNumberService.getUsedCancelled();
		if (usedCancelled.isPresent() && !usedCancelled.get().isEmpty()) {
			archiveAndDeleteCodeNumberList(usedCancelled.get());
		}
	}

	@Override
	public void updateOtpStatus() {
		Optional<List<CodeNumber>> allNotUsed = codeNumberService.getAllNotUsed();
		if (allNotUsed.isPresent()) {
			List<CodeNumber> codeNumberList = allNotUsed.get();
			codeNumberList.forEach(codeNumber -> {
				if (hasOtpReceived(codeNumber)) {
					codeNumber.setOtpReceived("Y");
					codeNumberService.saveAndFlush(codeNumber);
				}
			});
		}
	}

	private void refundUserIfNotUsed(Optional<List<CodeNumber>> allAfter20MinNotUsed) {
		synchronized (this) {
			if (allAfter20MinNotUsed.isPresent()) {

				List<CodeNumber> codeNumberList = allAfter20MinNotUsed.get();

				codeNumberList.forEach(c -> {
					String username = c.getUser();
					double price = c.getPrice();
					userService.refund(username, price, c.getCode());
					codeNumberService.deleteById(c.getCode()); // deleting from code_number
				});
			}
		}
	}

	private boolean archiveAndDeleteCodeNumberList(List<CodeNumber> codeNumberList) {
		List<CodeNumberArchive> codeNumberArchives = new ArrayList<>();
		codeNumberList.stream().peek(c -> codeNumberService.deleteById(c.getCode())) // delete first
				.filter(c -> "Y".equalsIgnoreCase(c.getOtpReceived())).forEach(c -> {
					CodeNumberArchive codeNumberArchive = new CodeNumberArchive();
					codeNumberArchive.setCode(c.getCode());
					codeNumberArchive.setNumber(c.getNumber());
					codeNumberArchive.setPrice(c.getPrice());
					codeNumberArchive.setServer(c.getServer());
					codeNumberArchive.setService(c.getService());
					codeNumberArchive.setUser(c.getUser());
					codeNumberArchive.setActualPrice(c.getActualPrice());
					codeNumberArchives.add(codeNumberArchive);
					codeNumberArchive.setArchiveTime(Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))));
				});

		archiveService.saveAllAndFlush(codeNumberArchives);
		return true;
	}

	private boolean hasOtpReceived(CodeNumber codeNumber) {
		String server = codeNumber.getServer();
		String result = getServer(server).getOtp(serverKeysService.getServerKey(server), codeNumber.getCode());

		return result.toUpperCase().contains("STATUS_OK");
	}

	@Override
	public void cancelNumbers(String username) {
		List<CodeNumber> allNumbers = codeNumberService.findActiveNumberByUsername(username);
		allNumbers.stream().forEach(cn -> {
			cancelActivation(cn.getCode(), cn.getServer());
		});

	}

}
