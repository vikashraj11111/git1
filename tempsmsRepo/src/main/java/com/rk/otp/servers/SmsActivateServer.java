package com.rk.otp.servers;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource(value = "classpath:servers/server2.properties")
public class SmsActivateServer implements ServerApi {

	@Value("${smsactivate.url.getBalance}")
	private String getBalanceUrl;

	@Value("${smsactivate.url.service.price}")
	private String getPriceUrl;

	@Value("${smsactivate.url.getnumber}")
	private String getNumberUrl;

	@Value("${smsactivate.url.number.getstatus}")
	private String getStatusUrl;

	@Value("${smsactivate.url.number.getfullsms}")
	private String getFullSmsUrl;

	@Value("${smsactivate.url.number.cancel}")
	private String cancelUrl;

	@Value("${smsactivate.url.getPriceAll}")
	private String getPriceAllUrl;

	@Value("${smsactivate.url.resendOtp}")
	private String resendUrl;

	private final RestTemplate template;

	public SmsActivateServer(RestTemplate template) {
		this.template = template;
	}

	@Override
	public String getBalance(String apiKey) {
		String result = template.getForObject(String.format(getBalanceUrl, apiKey), String.class);
		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";
		if (result == null || !result.contains(":"))
			return "";

		return result.split(":")[1];
	}

	@Override
	public String getPrice(String apiKey, String service) {
		String result = template.getForObject(String.format(getPriceUrl + service, apiKey), String.class);

		Number cost = 0;
		try {
			JSONObject jsonObject = new JSONObject(result);
			cost = jsonObject.getJSONObject("22").getJSONObject(service).getNumber("cost");
		} catch (JSONException e) {
			System.out.println("JsonException occurred : " + e);
		}

		return String.valueOf(cost);
	}

	@Override
	public String getNumber(String apiKey, String service) {
		String result = template.getForObject(String.format(getNumberUrl + service, apiKey), String.class);
		System.out.println(result);
		if ("NO_BALANCE".equalsIgnoreCase(result))
			return result;

		if (result != null) {
			if (!result.contains(":"))
				return "ERROR";

			String[] arr = result.split(":");
			if (arr[0].equalsIgnoreCase("BANNED"))
				return "ERROR";

			return arr[2].substring(2) + ":" + arr[1];
		}

		return "ERROR";
	}

	@Override
	public String getOtp(String apiKey, String code) {

		String result = template.getForObject(String.format(getStatusUrl + code, apiKey), String.class);
//		System.out.println("getOtp() :: " + result);
		if (result == null)
			return "ERROR";

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";

		if ("STATUS_CANCEL".equalsIgnoreCase(result) || "WRONG_ACTIVATION_ID".equalsIgnoreCase(result))
			return "CANCEL";

		if ("STATUS_WAIT_CODE".equalsIgnoreCase(result) || "STATUS_WAIT_RETRY".equalsIgnoreCase(result)
				|| "STATUS_WAIT_RESEND".equalsIgnoreCase(result) || result.toUpperCase().contains("STATUS_WAIT_RETRY"))
			return "WAITING";

		if ("NO_ACTIVATION".equalsIgnoreCase(result))
			return "NO_ACTIVATION";

		if (!(result.toUpperCase().contains("STATUS_OK") || result.toUpperCase().contains("FULL_SMS"))) {
			System.out.println("getOtp() !STATUS_OK :: " + result);
			return "ERROR_OCCURRED";
		}

		return result;
	}

	@Override
	public String getFullSms(String apiKey, String code) {

		String result = template.getForObject(String.format(getFullSmsUrl + code, apiKey), String.class);

		if (result == null)
			return null;

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";

		if ("STATUS_CANCEL".equalsIgnoreCase(result) || "WRONG_ACTIVATION_ID".equalsIgnoreCase(result))
			return "CANCEL";

		if ("STATUS_WAIT_CODE".equalsIgnoreCase(result) || "STATUS_WAIT_RETRY".equalsIgnoreCase(result)
				|| "STATUS_WAIT_RESEND".equalsIgnoreCase(result) || result.toUpperCase().contains("STATUS_WAIT_RETRY"))
			return "WAITING";

		if ("NO_ACTIVATION".equalsIgnoreCase(result))
			return "NO_ACTIVATION";

		if (!(result.toUpperCase().contains("STATUS_OK") || result.toUpperCase().contains("FULL_SMS"))) {
			System.out.println("getOtp() !FULL_SMS :: " + result);
			return "ERROR_OCCURRED";
		}

		return result;
	}

	@Override
	public String cancel(String apiKey, String code) {

		return template.getForObject(String.format(cancelUrl + code, apiKey), String.class);
	}

	@Override
	public String getPriceAll(String apiKey) {

		return template.getForObject(String.format(getPriceAllUrl, apiKey), String.class);
	}

	@Override
	public String resend(String apiKey, String code) {
		String result = template.getForObject(String.format(resendUrl + code, apiKey), String.class);

		if ("ACCESS_RETRY_GET".equalsIgnoreCase(result) || "BAD_STATUS".equalsIgnoreCase(result)) {
			System.out.println("waiting for a new SMS");
			return "Success";
		}

		return "Failed";
	}

	@Override
	public String getCount(String apiKey, String service) {
		String result = template.getForObject(String.format(getPriceUrl + service, apiKey), String.class);

		String count = "";
		try {
			JSONObject jsonObject = new JSONObject(result);
			count = jsonObject.getJSONObject("22").getJSONObject(service).get("count").toString();
		} catch (JSONException e) {
			System.out.println("JsonException occurred : " + e);
		}

		return count;
	}

	@Override
	public String getNumberByCountry(String apiKey, String service, String country) {
		// TODO Auto-generated method stub
		return null;
	}

}
