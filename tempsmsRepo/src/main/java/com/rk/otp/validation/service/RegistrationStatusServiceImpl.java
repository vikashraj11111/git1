package com.rk.otp.validation.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import com.rk.otp.validation.utility.Utility;

import reactor.core.publisher.Mono;

@Service
@PropertySource(value = "classpath:validation.properties", ignoreResourceNotFound = true)
public class RegistrationStatusServiceImpl implements RegistrationStatusService {

	@Autowired
	private RestTemplate template;

	@Autowired
	private WebClient webClient;

	@Value("${url.registration.status.flipkart}")
	private String flipkartUrl;

	@Value("${url.registration.status.dream11}")
	private String dream11Url;

	@Value("${url.registration.status.swiggy}")
	private String swiggyUrl;

	@Value("${url.registration.status.myntra}")
	private String myntraUrl;

	@Value("${url.registration.status.paytm-auth}")
	private String paytmAuthUrl;

	@Value("${url.registration.status.paytm}")
	private String paytmUrl;

	private final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
	private final String SWIGGY_COOKIE_1 = "lng=s%3A77.11469986756225.9MQKulc83JXaC8NjPjy3s88RcHW6k%2Bj0n4TVAlzgsUY; addressId=s%3Aundefined.H4tl815DCZ6%2Fo5v13eAn2NGFexz2evmgMlUcBAJMXS8; LocSrc=s%3AseoCty.Ln0AZvvgsCPuKj5IAqQw2NnSsBG0BSCZyoU1v9a5kXs; _sid=";
	private final String SWIGGY_COOKIE_2 = ";";
	private final int MAX_RETRY = 10;

	private static String sessionToken = null;
	private static String authToken = null;

	private static Map<String, String> mmtCookieMap = new HashMap<>();

	@Override
	public boolean checkFlipkartRegistrationStatus(String number) {
		number = "+91" + number;

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("X-User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36 FKUA/website/42/website/Desktop");

		JSONObject requestJsonObject = new JSONObject();
		requestJsonObject.put("loginId", Arrays.asList(number));
		requestJsonObject.put("supportAllStates", true);
		HttpEntity<String> request = new HttpEntity<>(requestJsonObject.toString(), headers);

		String result = template.postForObject(flipkartUrl, request, String.class);

		JSONObject responseJsonObject = new JSONObject(result);
		System.out.println("response responseJsonObject for number " + number + " :: " + responseJsonObject);
		String status = responseJsonObject.getJSONObject("RESPONSE").getJSONObject("userDetails").getString(number);
		System.out.println(status);
		if (!"NOT_FOUND".equalsIgnoreCase(status))
			return true;

		return false;
	}

	@Override
	public boolean checkDream11RegistrationStatus(String number) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		System.out.println("Checking Dream11 registration status for :: " + number);
		String requestJsonString = "{\"query\":\"mutation login($mobileNumber: String!, $site: String) "
				+ "{ loginGetOTP(mobileNumber: $mobileNumber, site: $site) { message }}\",\"variables\":{\"mobileNumber\":\"%s\"}}";

		JSONObject requestJsonObject = new JSONObject(String.format(requestJsonString, number));
		HttpEntity<String> request = new HttpEntity<>(requestJsonObject.toString(), headers);

		String result = template.postForObject(dream11Url, request, String.class);
		JSONObject responseJsonObject = new JSONObject(result);
		System.out.println("response responseJsonObject for number " + number + " :: " + responseJsonObject);
		if (responseJsonObject.has("errors")) {
			String errorCode = responseJsonObject.getJSONArray("errors").getJSONObject(0).getJSONObject("error")
					.getString("MsgCode");
			if ("MG010T".equalsIgnoreCase(errorCode)) {
				return false;
			}

		}

		return true;
	}

	@Override
	public boolean checkSwiggyRegistrationStatus(String number, int retryCount) {
		System.out.println("Inside checkSwiggyRegistrationStatus start");

		if (retryCount > 15) {
			return false;
		}
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("mobile", number);
		ResponseEntity<String> responseEntity = webClient.post().uri(swiggyUrl).headers(headers -> {
			headers.setOrigin("https://www.swiggy.com");
			headers.add("__fetch_req__", "true");
			headers.add("user-agent", USER_AGENT);
			headers.add("cookie", SWIGGY_COOKIE_1 + UUID.randomUUID().toString() + SWIGGY_COOKIE_2);
		}).body(Mono.just(map), MultiValueMap.class).retrieve().toEntity(String.class).timeout(Duration.ofMillis(1000L))
				.retry(2).block();

		JSONObject responseJsonObject = new JSONObject(responseEntity.getBody());

		if (responseJsonObject.getInt("statusCode") == 429) {
			return checkSwiggyRegistrationStatus(number, ++retryCount);
		}

		if (responseJsonObject.has("data")) {
			return responseJsonObject.getJSONObject("data").getBoolean("registered");
		}

		return false;
	}

	@Override
	public boolean checkMyntraRegistrationStatus(String number) {
		System.out.println("Inside checkMyntraRegistrationStatus start");

		ResponseEntity<String> entity = webClient.get()
				.uri("https://www.myntra.com/forgot?referer=https%3A%2F%2Fwww.myntra.com%2F").retrieve()
				.toEntity(String.class).timeout(Duration.ofMillis(5000L)).retry(2).block();

		System.out.println("Entity :: " + entity);
		System.out.println("Entity.getHeaders :: " + entity.getHeaders());

		Map<String, String> cookies = entity.getHeaders().get("Set-Cookie").stream().map(Utility::extractCookie)
				.collect(Collectors.toMap(Utility::extractCookieName, Utility::extractCookieValue));

		String cookie = cookies.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
				.collect(Collectors.joining("; "));
		cookie += "; AKA_A2=A";
		;
		cookies.put("newCookie", cookie);

		JSONObject responseJsonObject;
		try {
			Mono<String> response = webClient.get().uri(myntraUrl + number).headers(header -> {
				header.add("cookie", cookies.get("newCookie"));
				header.add("referrer", "https://www.myntra.com/forgot");
				header.add("origin", "https://www.myntra.com");
				header.add("x-myntraweb", "Yes");
				header.add("x-requested-with", "browser");
				header.add("x-location-context", "pincode=700018;source=IP");
				header.add("deviceId", cookies.get("_d_id"));
				header.add("x-meta-app", "deviceId=" + cookies.get("_d_id")
						+ ";appFamily=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36;reqChannel=web;channel=web;");
				header.add("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");
				header.add("sec-ch-ua-mobile", "?0");
				header.add("sec-ch-ua-platform", "\"Windows\"");
				header.add("sec-fetch-dest", "empty");
				header.add("sec-fetch-mode", "cors");
				header.add("sec-fetch-site", "same-origin");
				header.add("accept-language", "en-US,en;q=0.9");
			}).retrieve().bodyToMono(String.class).timeout(Duration.ofMillis(5000L)).retry(2);

			responseJsonObject = new JSONObject(response.block());
		} catch (WebClientException e1) {
			System.out.println("Exception occurred :: " + e1);
			return false;
		}

		System.out.println(responseJsonObject);

		return true;
	}

	@Override
	public boolean checkPaytmRegistrationStatus(String number) {
		System.out.println("Inside checkPaytmRegistrationStatus start");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String requestJsonString = "{\"clientId\":\"paytm-unified-merchant-panel\",\"responseType\":\"code\",\"scope\":\"paytm\",\"redirectUri\":\"https://dashboard.paytm.com/auth\"}";

		HttpEntity<String> request = new HttpEntity<>(requestJsonString, headers);
		String result = template.postForObject(paytmAuthUrl, request, String.class);
		String csrfToken = new JSONObject(result).getJSONObject("data").getString("authState");

		requestJsonString = "{\"mobile\":\"" + number
				+ "\",\"loginPassword\":\"dasdfas\",\"dob_agreement\":true,\"scope\":\"paytm\",\"responseType\":\"code\",\"redirectUri\":"
				+ "\"https://dashboard.paytm.com/auth\",\"clientId\":\"paytm-unified-merchant-panel\",\"csrfToken\":\""
				+ csrfToken + "\"}";
		request = new HttpEntity<>(requestJsonString, headers);
		result = template.postForObject(paytmUrl, request, String.class);
		JSONObject responseJsonObject = new JSONObject(result);

		if (responseJsonObject.getString("responseCode").equals("707"))
			return true;

		return false;
	}

//	@Override
//	public String checkLazyPaySimplEligibilityMakeMyTrip(String number, int retry, String bankName)
//			throws RestClientException, URISyntaxException {
//		if (retry >= MAX_RETRY)
//			return "ERROR";
//
//		String returnMsg = "NOT_ELIGIBLE";
//		String url = "https://payments.makemytrip.com/api/payments/payLaterEligibility";
//		String url2 = "https://railways.makemytrip.com/api/booking/review";
//
//		String data = "{\"checkoutId\":\"%s\",\"mobile\":\"%s\",\"payMode\":\"PL\",\"payLaterRequestType\":\"PL_PAY_OPTION\"}";
//		String data2 = "{\"reservationChoice\":\"99\",\"wsUserLogin\":\"rahul4luv\",\"mobileNumber\":\"7000623977\",\"email\":\"rkifjfhush@fgdf.gdfg\",\"boardingStation\":\"PNBE\",\"passengerList\":[{\"childBerthFlag\":false,\"passengerAge\":20,\"passengerBedrollChoice\":false,\"passengerBerthChoice\":\"\",\"passengerGender\":\"M\",\"passengerIcardFlag\":false,\"passengerName\":\"Rahul \",\"passengerSerialNumber\":1,\"passengerNationality\":\"IN\",\"bedRollFlagEnabled\":\"false\"}],\"autoUpgradationSelected\":false,\"confirmBookCheckbox\":false,\"travelInsuranceOpted\":false,\"bookMode\":\"D\",\"journeyDetails\":{\"source\":\"PNBE\",\"destination\":\"GAYA\",\"class\":\"CC\",\"quota\":\"GN\",\"doj\":\"20230330\",\"trainDetails\":{\"trainNumber\":\"12365\",\"departureTime\":\"06:10\"},\"departure_time\":1680136800000,\"arrival_time\":1680143100000,\"duration\":105,\"frmCityName\":\"Patna Junction\",\"fromStationName\":\"Patna Junction\",\"toStationName\":\"Gaya Junction\",\"toCityName\":\"Gaya Junction\",\"advance_purchase\":91},\"mmtAuth\":null,\"myBusinessSubscription\":null,\"psgnDestinationAddress\":{\"address\":\"\",\"street\":\"\",\"colony\":\"\",\"city\":\"\",\"postOffice\":\"\",\"state\":\"\",\"pinCode\":null},\"alternateAvailabilityBooking\":false,\"clusterTrain\":false,\"bookingSource\":\"\",\"departure_time\":1680136800000,\"arrival_time\":1680143100000,\"duration\":105,\"frmCityName\":\"Patna Junction\",\"fromStationName\":\"Patna Junction\",\"toStationName\":\"Gaya Junction\",\"toCityName\":\"Gaya Junction\",\"advance_purchase\":91,\"locus_info\":{\"source_locus_code\":null,\"source_locus_type\":\"CITY\",\"source_locus_city_code\":null,\"source_locus_station_code\":null,\"destination_locus_code\":null,\"destination_locus_type\":null,\"destination_locus_city_code\":\"CITY\",\"destination_locus_station_code\":null,\"destination_locus_station_name\":null},\"tracking_params\":{\"is_logged_in\":false,\"uuid\":null,\"profile_type\":null,\"email_communication_id\":null,\"mobile_communication_id\":null,\"is_email_verified\":null,\"is_mobile_verified\":null,\"email_id\":null,\"mobile_country_code\":null,\"trackingId\":null,\"userIP\":null,\"requestingServerIP\":null,\"respondingServerIP\":null,\"bookMode\":\"D\",\"marketing_cloud_id\":null,\"login_channel\":null},\"device_info\":{\"browser\":\"Chrome\",\"browser_version\":\"108\",\"operating_system\":\"Windows\",\"operating_system_version\":\"NA\",\"model\":null,\"id\":null,\"carrier\":null,\"latitude\":null,\"longitude\":null,\"connection_type\":\"WiFi\",\"application_version\":null,\"geo_city\":null,\"user_agent\":null,\"flavour\":\"desktop\",\"device_id\":\"96ea2a2d-0d71-49ac-a4a8-b61f362f5d7f\",\"device_type\":\"desktop\",\"lang\":null},\"cnfmGuaranteeDetail\":{\"cnfmGuaranteeOpted\":false,\"railofyShown\":false,\"zcShown\":true,\"railsConfirmationGuaranteeOption\":1},\"freeCancellationInsuranceDetail\":{\"freeCancellationInsuranceOpted\":false}}";
//
//		try {
//			JSONObject response = sendPOST(url2, data2); // To get checkout id
//			if (!response.has("error")) {
//				String checkoutId = response.getJSONObject("paymentInfo").getString("checkoutId");
//
//				response = sendPOST(url, String.format(data, checkoutId, number)); // To get eligibility
//				if (!response.has("error")) {
//					JSONArray jsonArray = response.getJSONArray("eligibilityDetails");
//					for (int i = 0; i < jsonArray.length(); i++) {
//						JSONObject jsonObject = jsonArray.getJSONObject(i);
//						String bank = jsonObject.getString("bank");
//						if (bank.equalsIgnoreCase(bankName))
//							returnMsg = "ELIGIBLE";
//					}
//				}
//			} else {
//				checkLazyPaySimplEligibilityMakeMyTrip(number, ++retry, bankName);
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			returnMsg = "ERROR";
//		}
//		return returnMsg;
//	}

	@Override
	public String getMmtCheckoutPage(String number) {
		String checkoutId = getMmtReview(number);
		return "https://payments.makemytrip.com/ui/checkout/?id=" + checkoutId;
	}

	@Override
	public String checkLazyPaySimplEligibilityMakeMyTrip(String number, int retry, String bankName) {
		if (retry >= MAX_RETRY)
			return "ERROR";

		String returnMsg = "NOT_ELIGIBLE";
		getMmtHomePage();
		String checkoutId = getMmtReview(number);
		checkEligibility(checkoutId, number);

		return returnMsg;
	}

	private void checkEligibility(String checkoutId, String number) {
		String eligibilityUrl = "https://payments.makemytrip.com/api/payments/payLaterEligibility";

		String eligibilityData = "{\"checkoutId\":\"%s\",\"mobile\":\"%s\",\"payMode\":\"PL\",\"payLaterRequestType\":\"PL_PAY_OPTION\"}";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("X-User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36 FKUA/website/42/website/Desktop");
		headers.add("Cookie", getMmtCookie());
		HttpEntity<String> request = new HttpEntity<>(String.format(eligibilityData, checkoutId, number), headers);

		ResponseEntity<String> entity = template.postForEntity(eligibilityUrl, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());
		System.out.println(jsonObject);
	}

	private String getMmtCookie() {
		String cookie = mmtCookieMap.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
				.reduce((x, y) -> x + "; " + y).get();
		return cookie;
	}

	/**
	 * @return String checkoutId
	 **/
	private String getMmtReview(String number) {
		String reviewUrl = "https://railways.makemytrip.com/api/booking/review";
		String reviewData = "{\r\n" + "  \"reservationChoice\": \"99\",\r\n" + "  \"wsUserLogin\": \"rahul\",\r\n"
				+ "  \"mobileNumber\": \"%s\",\r\n" + "  \"email\": \"isfsd3fkdsfjksd@gmail.com\",\r\n"
				+ "  \"boardingStation\": \"PNBE\",\r\n" + "  \"passengerList\": [\r\n" + "    {\r\n"
				+ "      \"childBerthFlag\": false,\r\n" + "      \"passengerAge\": 22,\r\n"
				+ "      \"passengerBedrollChoice\": false,\r\n" + "      \"passengerBerthChoice\": \"\",\r\n"
				+ "      \"passengerGender\": \"M\",\r\n" + "      \"passengerIcardFlag\": false,\r\n"
				+ "      \"passengerName\": \"saasfas \",\r\n" + "      \"passengerSerialNumber\": 1,\r\n"
				+ "      \"passengerNationality\": \"IN\",\r\n" + "      \"bedRollFlagEnabled\": \"false\"\r\n"
				+ "    }\r\n" + "  ],\r\n" + "  \"autoUpgradationSelected\": false,\r\n"
				+ "  \"confirmBookCheckbox\": false,\r\n" + "  \"travelInsuranceOpted\": false,\r\n"
				+ "  \"bookMode\": \"D\",\r\n" + "  \"journeyDetails\": {\r\n" + "    \"source\": \"PNBE\",\r\n"
				+ "    \"destination\": \"GAYA\",\r\n" + "    \"class\": \"CC\",\r\n" + "    \"quota\": \"GN\",\r\n"
				+ "    \"doj\": \"20240920\",\r\n" + "    \"trainDetails\": {\r\n"
				+ "      \"trainNumber\": \"12365\",\r\n" + "      \"departureTime\": \"06:10\"\r\n" + "    },\r\n"
				+ "    \"departure_time\": 1726792800000,\r\n" + "    \"arrival_time\": 1726799100000,\r\n"
				+ "    \"duration\": 105,\r\n" + "    \"frmCityName\": \"Patna Jn\",\r\n"
				+ "    \"fromStationName\": \"Patna Jn\",\r\n" + "    \"toStationName\": \"Gaya Jn\",\r\n"
				+ "    \"toCityName\": \"Gaya Jn\",\r\n" + "    \"advance_purchase\": 74\r\n" + "  },\r\n"
				+ "  \"mmtAuth\": null,\r\n" + "  \"myBusinessSubscription\": null,\r\n"
				+ "  \"psgnDestinationAddress\": {\r\n" + "    \"address\": \"\",\r\n" + "    \"street\": \"\",\r\n"
				+ "    \"colony\": \"\",\r\n" + "    \"city\": \"\",\r\n" + "    \"postOffice\": \"\",\r\n"
				+ "    \"state\": \"\",\r\n" + "    \"pinCode\": null\r\n" + "  },\r\n"
				+ "  \"clusterTrain\": false,\r\n" + "  \"bookingSource\": \"\",\r\n"
				+ "  \"departure_time\": 1726792800000,\r\n" + "  \"arrival_time\": 1726799100000,\r\n"
				+ "  \"duration\": 105,\r\n" + "  \"frmCityName\": \"Patna Jn\",\r\n"
				+ "  \"fromStationName\": \"Patna Jn\",\r\n" + "  \"toStationName\": \"Gaya Jn\",\r\n"
				+ "  \"toCityName\": \"Gaya Jn\",\r\n" + "  \"advance_purchase\": 74,\r\n" + "  \"locus_info\": {\r\n"
				+ "    \"source_locus_code\": null,\r\n" + "    \"source_locus_type\": \"CITY\",\r\n"
				+ "    \"source_locus_city_code\": null,\r\n" + "    \"source_locus_station_code\": null,\r\n"
				+ "    \"destination_locus_code\": null,\r\n" + "    \"destination_locus_type\": null,\r\n"
				+ "    \"destination_locus_city_code\": \"CITY\",\r\n"
				+ "    \"destination_locus_station_code\": null,\r\n"
				+ "    \"destination_locus_station_name\": null\r\n" + "  },\r\n" + "  \"tracking_params\": {\r\n"
				+ "    \"is_logged_in\": false,\r\n" + "    \"uuid\": null,\r\n" + "    \"profile_type\": null,\r\n"
				+ "    \"email_communication_id\": null,\r\n" + "    \"mobile_communication_id\": null,\r\n"
				+ "    \"is_email_verified\": null,\r\n" + "    \"is_mobile_verified\": null,\r\n"
				+ "    \"email_id\": null,\r\n" + "    \"mobile_country_code\": null,\r\n"
				+ "    \"trackingId\": null,\r\n" + "    \"userIP\": null,\r\n"
				+ "    \"requestingServerIP\": null,\r\n" + "    \"respondingServerIP\": null,\r\n"
				+ "    \"bookMode\": \"D\",\r\n" + "    \"marketing_cloud_id\": null,\r\n"
				+ "    \"login_channel\": null\r\n" + "  },\r\n" + "  \"device_info\": {\r\n"
				+ "    \"browser\": \"Chrome\",\r\n" + "    \"browser_version\": \"126\",\r\n"
				+ "    \"operating_system\": \"Windows\",\r\n" + "    \"operating_system_version\": \"NA\",\r\n"
				+ "    \"model\": null,\r\n" + "    \"id\": null,\r\n" + "    \"carrier\": null,\r\n"
				+ "    \"latitude\": null,\r\n" + "    \"longitude\": null,\r\n"
				+ "    \"connection_type\": \"WiFi\",\r\n" + "    \"application_version\": null,\r\n"
				+ "    \"geo_city\": null,\r\n" + "    \"user_agent\": null,\r\n" + "    \"flavour\": \"desktop\",\r\n"
				+ "    \"device_id\": \"51dc06cc-ff96-4ef2-b395-6168ac0092ef\",\r\n"
				+ "    \"device_type\": \"desktop\",\r\n" + "    \"lang\": null\r\n" + "  },\r\n"
				+ "  \"cnfmGuaranteeDetail\": {\r\n" + "    \"cnfmGuaranteeOpted\": false,\r\n"
				+ "    \"railofyShown\": false,\r\n" + "    \"zcShown\": true,\r\n"
				+ "    \"railsConfirmationGuaranteeOption\": 1\r\n" + "  },\r\n"
				+ "  \"freeCancellationInsuranceDetail\": {\r\n" + "    \"freeCancellationInsuranceOpted\": false\r\n"
				+ "  },\r\n" + "  \"gstinBillingAddress\": {\r\n" + "    \"state\": \"Karnataka\"\r\n" + "  }\r\n"
				+ "}";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("X-User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36 FKUA/website/42/website/Desktop");

		HttpEntity<String> request = new HttpEntity<>(String.format(reviewData, number), headers);

		ResponseEntity<String> entity = template.postForEntity(reviewUrl, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());
		String checkoutId = jsonObject.getJSONObject("paymentInfo").getString("checkoutId");
		return checkoutId;
	}

	private String getMmtHomePage() {
		String homePageUrl = "https://www.makemytrip.com/";
		ResponseEntity<String> entity = webClient.get().uri(homePageUrl).retrieve().toEntity(String.class)
				.timeout(Duration.ofMillis(5000L)).retry(2).block();
		List<String> list = entity.getHeaders().get("Set-Cookie");
		System.out.println(list);
		setMmtCookie(entity);

		return null;
	}

	private void setMmtCookie(ResponseEntity<String> entity) {
		List<String> list = entity.getHeaders().get("Set-Cookie");
		list.forEach(str -> {
			String cookie = Utility.extractCookie(str);
			System.out.println(cookie);
			mmtCookieMap.put(Utility.extractCookieName(cookie), Utility.extractCookieValue(cookie));
		});
		System.out.println(mmtCookieMap);
	}

	@Override
	public String checkLazyPaySimplEligibilityPayU(String number, int amount, String bankName, String paymentId,
			String accessToken) {
		String returnMsg = "NOT_ELIGIBLE";
		String url = "https://api.payu.in/emi/customerEligibility";

		String data = "{\"bankCode\":\"%s\",\"paymentId\":\"" + paymentId + "\",\"accessToken\":\"" + accessToken
				+ "\"," + "\"paymentDetails\":{\"amount\":%s,\"mobileNumber\":\"%s\"}}";

		if ("LAZYPAY".equalsIgnoreCase(bankName))
			data = String.format(data, "LAZYPAY", amount, number);
		else if ("SIMPL".equalsIgnoreCase(bankName))
			data = String.format(data, "SIMPL", amount, number);

		try {
			JSONObject responseJsonObject = sendPOST(url, data);
			System.out.println(responseJsonObject);
			final List<String> TEXT_LIST = Arrays.asList("null", "Not Eligible", "No Error",
					"This mobile number is not eligible. Please change the mobile number.");

			if (responseJsonObject.has("status") && responseJsonObject.getBoolean("status")) {
				returnMsg = "ELIGIBLE";
			} else if (responseJsonObject.has("error") && TEXT_LIST.stream().noneMatch(s -> String
					.valueOf(responseJsonObject.getJSONObject("error").get("message")).equalsIgnoreCase(s))) {
				returnMsg = "ERROR";
			}
		} catch (IOException e) {
			System.err.println("IOException occurred :: " + e.getMessage());
			returnMsg = "ERROR";
		} catch (Exception ex) {
			System.err.println("Exception occurred :: " + ex.getMessage());
			returnMsg = "ERROR";
		}
		return returnMsg;
	}

	private JSONObject sendPOST(String url, String data) throws IOException {
		StringBuffer response = new StringBuffer();
//		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("117.240.206.222", 80));
		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Connection", "keep-alive");
		con.setRequestProperty("origin", "https://www.makemytrip.com");
		con.setRequestProperty("referer", "https://www.makemytrip.com");
		con.setRequestProperty("session-id", "96ea2a2d-0d71-49ac-a4a8-b61f362f5d7f_dweb_1672321116073_629d");
		con.setRequestProperty("accept-language", "en-US,en;q=0.9,hi;q=0.8");
		con.setRequestProperty("language", "eng");

		// For POST only - START
		con.setDoOutput(true);
		OutputStream os = con.getOutputStream();
		byte[] input = data.getBytes("utf-8");
		os.write(input, 0, input.length);
		os.flush();
		os.close();
		// For POST only - END

		int responseCode = con.getResponseCode();

		if (responseCode == HttpsURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} else {
			System.out.println("POST request did not work.");
		}
		return new JSONObject(response.toString());
	}

	@Override
	public String checkLazyPaySimplEligibilityRazor(String number, int amount, String keyId, boolean isSimpl,
			boolean isLazyPay) {
		String returnMsg = "NOT_ELIGIBLE";
		try {
			String response = getRazorPayResponse(amount, number, keyId, sessionToken, 0, isSimpl, isLazyPay);
			JSONObject responseJsonObject = new JSONObject(response);
			System.out.println(responseJsonObject);
			if (responseJsonObject.has("instruments")) {
				Iterator<Object> iterator = responseJsonObject.getJSONArray("instruments").iterator();
				while (iterator.hasNext()) {
					JSONObject responseJson = ((JSONObject) iterator.next());
					returnMsg = responseJson.getJSONObject("eligibility").getString("status");
					if ("ineligible".equalsIgnoreCase(returnMsg)) {
						if ("transaction_suspended".equalsIgnoreCase(
								responseJson.getJSONObject("eligibility").getJSONObject("error").getString("reason"))) {
							returnMsg = "ERROR";
							break;
						} else
							returnMsg = "NOT_ELIGIBLE";
					} else if ("failed".equalsIgnoreCase(returnMsg))
						returnMsg = "EXHAUSTED";
					else {
						returnMsg = "ELIGIBLE";
						break;
					}
				}
			} else
				returnMsg = "ERROR";
		} catch (Exception e) {
			System.out.println("Exception occurred :: " + e.getMessage());
			returnMsg = "ERROR";
		}

		return returnMsg;
	}

	private String getRazorPayResponse(int amount, String number, String keyId, String token, int retry,
			boolean getSimpl, boolean getLazyPay) throws InterruptedException {
		String url = "https://api.razorpay.com/v1/standard_checkout/public/customers/eligibility?session_token=%s&key_id=%s";
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("amount", String.valueOf(amount * 100));
		map.add("currency", "INR");
		map.add("customer[contact]", "+91" + number);
		map.add("instruments[0][method]", "paylater");
		if (getSimpl && getLazyPay) {
			map.add("instruments[0][providers][0]", "getsimpl");
			map.add("instruments[0][providers][1]", "lazypay");
		} else if (getSimpl)
			map.add("instruments[0][providers][0]", "getsimpl");
		else if (getLazyPay)
			map.add("instruments[0][providers][0]", "lazypay");

		System.out.println(map);
		ResponseEntity<String> entity = webClient.post().uri(String.format(url, token, keyId))
				.body(Mono.just(map), MultiValueMap.class).retrieve()
				.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (r) -> Mono.empty()).toEntity(String.class)
				.timeout(Duration.ofMillis(5000L)).retry(2).block();

		if (entity != null && entity.getStatusCode().value() == 401 && retry == 0) {
			return getRazorPayResponse(amount, number, keyId, getSessionToken(), ++retry, getSimpl, getLazyPay);
		}

		return entity.getBody();
	}

	private String getSessionToken() {
		String token = null;
		ResponseEntity<String> entity = webClient.get().uri(
				"https://api.razorpay.com/v1/checkout/public?traffic_env=production&build=ad42b17ef474f9f1d8212bf50544188037ca78e0&modern=1&unified_lite=1")
				.retrieve().toEntity(String.class).timeout(Duration.ofMillis(5000L)).retry(2).block();

		System.out.println(entity.getHeaders().getLocation().getQuery());
		for (String pair : entity.getHeaders().getLocation().getQuery().split("&")) {
			if (pair.startsWith("session_token")) {
				token = pair.substring(pair.indexOf("=") + 1);
				sessionToken = token;
			}
		}

		return token;
	}

	@Override
	public int checkLazyPaySimplEligibilityAmount(String number, String keyId, boolean isSimpl, boolean isLazyPay) {
		int eligibleAmount = 0;
		String tempEligibility = "";
		int tempAmount = 0;
		if (isSimpl) {
			tempEligibility = checkLazyPaySimplEligibilityRazor(number, 500, keyId, isSimpl, isLazyPay);
			if ("ELIGIBLE".equals(tempEligibility)) {
				try {
					TimeUnit.MILLISECONDS.sleep(150);
					tempEligibility = checkLazyPaySimplEligibilityRazor(number, 20000, keyId, isSimpl, isLazyPay);
					if ("ELIGIBLE".equalsIgnoreCase(tempEligibility)) {
						return 0;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		do {
			try {
				TimeUnit.MILLISECONDS.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			tempAmount += 250;
			tempEligibility = checkLazyPaySimplEligibilityRazor(number, tempAmount, keyId, isSimpl, isLazyPay);
			System.out.println(number + " - " + tempEligibility);
			if ("ELIGIBLE".equals(tempEligibility)) {
				eligibleAmount = tempAmount;
			}

		} while (tempEligibility.equalsIgnoreCase("ELIGIBLE"));

		return eligibleAmount;
	}

	@Override
	public String checkLazyPayEligibilityCashFree(String number) {

		String orderReference = getOrderReference();
		String sessionId = getSession(orderReference);
		if (sessionId == null) {
			return "ERROR";
		}
		String url = "https://payments.cashfree.com/pgbillpayuiapi/paylater/eligibility/lazypayv2/" + sessionId;
		String data = String.format("{\"customerPhone\":\"+91%s\"}", number);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(data, headers);

		ResponseEntity<String> entity = template.postForEntity(url, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());
		String status = jsonObject.getString("status");
		if ("SUCCESS".equals(status)) {
			String message = jsonObject.getJSONObject("message").getString("message");
			if ("eligible".equalsIgnoreCase(message)) {
				return "ELIGIBLE";
			} else if ("not_eligible".equalsIgnoreCase(message)) {
				return "NOT_ELIGIBLE";
			} else {
				return "ERROR";
			}
		}
		return "ERROR";
	}

	private String getSession(String orderReference) {
		String url = "https://api.cashfree.com/pg/view/sessions/checkout";
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.add("payment_session_id", orderReference);
		map.add("browser_meta",
				"eyJ1c2VyQWdlbnQiOiJNb3ppbGxhLzUuMCAoV2luZG93cyBOVCAxMC4wOyBXaW42NDsgeDY0KSBBcHBsZVdlYktpdC81MzcuMzYgKEtIVE1MLCBsaWtlIEdlY2tvKSBDaHJvbWUvMTIwLjAuMC4wIFNhZmFyaS81MzcuMzYifQ==");
		map.add("platform", "js-ch");

		ResponseEntity<String> entity = webClient.post().uri(url).body(Mono.just(map), MultiValueMap.class).retrieve()
				.onStatus((s) -> s.equals(HttpStatus.UNAUTHORIZED), (r) -> Mono.empty()).toEntity(String.class)
				.timeout(Duration.ofMillis(5000L)).retry(2).block();

		List<String> list = entity.getHeaders().get("Set-Cookie");
		return list.stream().filter(c -> c.startsWith("cfp_pa_session_id")).limit(1).reduce((x, y) -> x.split("_")[0])
				.orElse(null);
	}

	private String getOrderReference() {
		String url = "https://api.shopflo.co/flo-checkout/api/checkout/v2/checkout/%s/payments/initiate";
		String orderId = getUID();
		String fullUrl = String.format(url, orderId);

		String data = "{\"payment_mode\":\"PAY_LATER\",\"action\":\"new_tab\",\"implementation\":\"DEFAULT\"}";
		if (authToken == null)
			authToken = getAuthToken();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("X-User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.84 Safari/537.36 FKUA/website/42/website/Desktop");
		headers.add("authorization", authToken);
		HttpEntity<String> request = new HttpEntity<>(data, headers);

		ResponseEntity<String> entity = template.postForEntity(fullUrl, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());
		System.out.println(jsonObject);

		return jsonObject.getString("pg_order_reference");
	}

	private String getAuthToken() {
		String url = "https://api.shopflo.co/heimdall/api/v1/otp/send";
		String data = "{\"oid\":\"+916000%s\",\"merchant_id\":\"e81093e8-3c71-493c-9cea-71fbcfa045a1\",\"skip_verification\":true,\"linked_user_id\":null,\"is_force_verification\":false}";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(String.format(data, new Random().nextInt(100000, 999999)),
				headers);

		ResponseEntity<String> entity = template.postForEntity(url, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());
		JSONObject dataObj = jsonObject.getJSONObject("data");
		if (dataObj.getBoolean("existing_customer"))
			return getAuthToken();

		return dataObj.getJSONObject("token").getString("access_token");
	}

	private String getUID() {
		String url = "https://api.shopflo.co/public/api/v1/tokens";
		String data = "{\"sf_session_id\":\"c71d774b-3e98-460e-a6ee-7938cf6aa428\",\"shop_url\":\"teatrunk.in\",\"data\":{\"checkout\":{\"items\":[{\"quantity\":1,\"item_id\":\"35442797445287\",\"price\":\"399.00\"}],\"discount_codes\":[],\"metadata\":{\"cart\":{\"token\":\"c1-4b1585bdcdc1248e17af945a0188a888\",\"note\":\"\",\"attributes\":{},\"original_total_price\":39900,\"total_price\":39900,\"total_discount\":0,\"total_weight\":500,\"item_count\":1,\"items\":[{\"id\":35442797445287,\"properties\":{},\"quantity\":1,\"variant_id\":35442797445287,\"key\":\"35442797445287:d37b8df7-fdac-4930-9307-b1db075561b7\",\"title\":\"Spearmint Leaves - 50 gms Pack\",\"price\":39900,\"original_price\":39900,\"discounted_price\":39900,\"line_price\":39900,\"original_line_price\":39900,\"total_discount\":0,\"discounts\":[],\"sku\":\"TTBSL100G\",\"grams\":500,\"vendor\":\"Tea Trunk\",\"taxable\":true,\"product_id\":5494426697895,\"product_has_only_default_variant\":false,\"gift_card\":false,\"final_price\":39900,\"final_line_price\":39900,\"url\":\"/products/spearmint-leaves?variant=35442797445287\",\"featured_image\":{\"aspect_ratio\":1,\"alt\":\"Spearmint Leaves\",\"height\":801,\"url\":\"https://cdn.shopify.com/s/files/1/0935/3276/files/01_5d9762ff-b200-4c6b-a353-0f80c2cafa0c.jpg?v=1705062311\",\"width\":801},\"image\":\"https://cdn.shopify.com/s/files/1/0935/3276/files/01_5d9762ff-b200-4c6b-a353-0f80c2cafa0c.jpg?v=1705062311\",\"handle\":\"spearmint-leaves\",\"requires_shipping\":true,\"product_type\":\"Botanicals > caffeine-free teas > zero caffeine > herbal tea > tisane > hormonal balance > PCOS > period relief\",\"product_title\":\"Spearmint Leaves\",\"product_description\":\"nA wonder herb for women, spearmint has properties known to reduce problems caused by hormonal imbalance. Glowing skin, good digestion and kissable breath - spearmint checks a lot of boxes!\",\"variant_title\":\"50 gms Pack\",\"variant_options\":[\"50 gms Pack\"],\"options_with_values\":[{\"name\":\"Choose\",\"value\":\"50 gms Pack\"}],\"line_level_discount_allocations\":[],\"line_level_total_discount\":0,\"has_components\":false}],\"requires_shipping\":true,\"currency\":\"INR\",\"items_subtotal_price\":39900,\"cart_level_discount_applications\":[]},\"note_attributes\":[{\"name\":\"landing_page\",\"value\":\"/collections/all-teas/products/spearmint-leaves\"},{\"name\":\"orig_referrer\",\"value\":\"https://teatrunk.in/collections/all-teas?campaignid=13356601595&adgroupid=122328961359&network=g&device=c&gad_source=1&gclid=CjwKCAiA44OtBhAOEiwAj4gpOcGcukEsO1cDgCydh0vbD-WkWhJF_Gfmr4GkiwCN75MO09n5r8rJrRoCDjsQAvD_BwE\"}],\"analytics\":{\"fb\":{\"fbp\":\"fb.1.1705074468253.1720092464\",\"fbc\":\"\"},\"moEngage\":{\"moe_uuid\":\"\"},\"webengage\":{\"anonymousId\":false,\"userIdType\":\"anonymousId\"},\"clevertap\":{\"ctid\":\"\"},\"ga\":{\"client_id\":\"1181697041.1705074467\",\"user_id\":\"b47aa622-d43a-4779-8edf-cc37aa3d1843\",\"session_ids\":[{\"key\":\"JJSLR7RE5E\",\"value\":\"GS1.1.1705074467.1.1.1705074470.0.0.0\"},{\"key\":\"KR35DS02R2\",\"value\":\"GS1.1.1705085264.2.0.1705085264.60.0.0\"}]}},\"user_details\":{},\"long_session_id\":\"b47aa622-d43a-4779-8edf-cc37aa3d1843\"}},\"redirect_url\":\"https://teatrunk.in/collections/all-teas/products/spearmint-leaves\",\"origin_url\":\"teatrunk.in\",\"is_buy_now\":false}}";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> request = new HttpEntity<>(data, headers);

		ResponseEntity<String> entity = template.postForEntity(url, request, String.class);

		JSONObject jsonObject = new JSONObject(entity.getBody());

		return jsonObject.getJSONObject("response").getString("uid");
	}

}
