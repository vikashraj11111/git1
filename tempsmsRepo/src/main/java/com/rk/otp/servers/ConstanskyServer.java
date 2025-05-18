package com.rk.otp.servers;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource(value = "classpath:servers/server6.properties")
public class ConstanskyServer implements ServerApi {

	@Value("${constansky.url.fetchtoken}")
	private String fetchTokenUrl;

	@Value("${constansky.url.getnumber}")
	private String getNumberUrl;

	@Value("${constansky.url.othercountry.getnumber}")
	private String getNumberByCountryUrl;

	@Value("${constansky.url.number.getstatus}")
	private String getStatusUrl;

	@Value("${constansky.url.number.cancel}")
	private String cancelUrl;

	private final RestTemplate template;

	public ConstanskyServer(RestTemplate template) {
		this.template = template;
	}

	@Override
	public String getNumber(String apiKey, String service) {
		System.out.println("Entered getNumber() ::");
		return getNumberByCountry(apiKey, service, "IN");
	}

	@Override
	public String getNumberByCountry(String apiKey, String service, String country) {
		System.out.println("Entered getNumberByCountry() ::");
		String token = fetchToken(apiKey);
		String url = String.format(getNumberByCountryUrl, country) + "&token=" + token + "&businessCode=" + service;

		String resultString = callRestUrl(url, 1);

		if (resultString.equals("NO_NUMBER") || resultString.equals("ERROR"))
			return resultString;

		JSONObject resultJson = new JSONObject(resultString);
		String message = resultJson.getString("message");
		String balance = resultJson.getJSONObject("data").getString("balance");

		if (Double.parseDouble(balance) < 1) {
			return "NO_BALANCE";
		}

		if ("Success".equalsIgnoreCase(message)) {

			JSONObject phNo = resultJson.getJSONObject("data").getJSONArray("phoneNumber").getJSONObject(0);

			String number = phNo.getString("number");
			String code = phNo.getString("serialNumber");

			System.out.println(number + ":" + code);

			return number.substring(3) + ":" + code + ":" + balance;
		}

		return "ERROR";
	}

	@Override
	public String getOtp(String apiKey, String code) {

		String token = fetchToken(apiKey);
		String url = getStatusUrl + "token=" + token + "&serialNumber=" + code;
		String resultString = template.getForObject(url, String.class);
		JSONObject resultJson = new JSONObject(resultString);
		int responseCode = Integer.parseInt(resultJson.getString("code"));

		if (responseCode != 200) {
			return "STATUS_CANCEL";
		}

		String result = resultJson.getJSONObject("data").getJSONArray("verificationCode").getJSONObject(0)
				.getString("vc");

		if (!StringUtils.hasText(result)) {
			System.out.println("getOtp() status waiting:: resultString : " + resultString);
			return "WAITING";
		}

		return result;
	}

	@Override
	public String cancel(String apiKey, String code) {
		String token = fetchToken(apiKey);

		String url = cancelUrl + "&token=" + token + "&serialNumber=" + code;
		String resultString = template.getForObject(url, String.class);

		String result = new JSONObject(resultString).getString("message");
		if ("Success".equalsIgnoreCase(result)) {
			return "ACCESS_CANCEL";
		} else {
			return "NO_ACTIVATION";
		}
	}

	private String fetchToken(String apiKey) {
		System.out.println("Entered fetchToken() :: ");
		String tokenString = template.getForObject(String.format(fetchTokenUrl, apiKey), String.class);
		String token = new JSONObject(tokenString).getJSONObject("data").getString("token");
		return token;
	}

	private String callRestUrl(String url, int count) {
		String resultString = template.getForObject(url, String.class);
		System.out.println("callRestUrl() :: count = " + count + ", resultString = " + resultString);

		JSONObject resultJson = new JSONObject(resultString);
		int responseCode = Integer.parseInt(resultJson.getString("code"));
		if (responseCode != 200) {
			if (count++ >= 5)
				return responseCode == 221 ? "NO_NUMBER" : "ERROR";
			System.out.println("responseCode != 200 :: responseCode = " + responseCode);
			System.out.println("retrying :: count : " + count);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("Exception occurred while Thread.sleep() :: " + e);
			}
			return callRestUrl(url, count);
		}

		return resultString;
	}

	@Override
	public String getBalance(String apiKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPrice(String apiKey, String service) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFullSms(String apiKey, String code) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPriceAll(String apiKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCount(String apiKey, String service) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resend(String apiKey, String code) {
		// TODO Auto-generated method stub
		return null;
	}

}
