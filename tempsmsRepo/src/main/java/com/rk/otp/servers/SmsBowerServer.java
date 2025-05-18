package com.rk.otp.servers;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource(value = "classpath:servers/server3.properties")
public class SmsBowerServer implements ServerApi {

	@Value("${smsbower.url.getBalance}")
	private String getBalanceUrl;

	@Value("${smsbower.url.service.price}")
	private String getPriceUrl;

	@Value("${smsbower.url.getnumber}")
	private String getNumberUrl;

	@Value("${smsbower.url.number.getstatus}")
	private String getStatusUrl;

	@Value("${smsbower.url.number.getfullsms}")
	private String getFullSmsUrl;

	@Value("${smsbower.url.number.cancel}")
	private String cancelUrl;

	@Value("${smsbower.url.getPriceAll}")
	private String getPriceAllUrl;

	@Value("${smsbower.url.resendOtp}")
	private String resendUrl;

	private final RestTemplate template;

	public SmsBowerServer(RestTemplate template) {
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
		Double cost = null;
		try {
			JSONObject jsonObject = new JSONObject(result).getJSONObject("22").getJSONObject(service);
			cost = jsonObject.getDouble("cost");
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

			return arr[2].substring(2) + ":" + arr[1];
		}

		return "ERROR";
	}

	@Override
	public String getOtp(String apiKey, String code) {

		String result = template.getForObject(String.format(getStatusUrl + code, apiKey), String.class);
		System.out.println("getOtp() for code:" + code + " :: " + result);
		if (result == null)
			return "ERROR";

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";

		if ("STATUS_CANCEL".equalsIgnoreCase(result))
			return "CANCEL";

		if ("STATUS_WAIT_CODE".equalsIgnoreCase(result))
			return "WAITING";

		if ("NO_ACTIVATION".equalsIgnoreCase(result))
			return "NO_ACTIVATION";

		if (!result.toUpperCase().contains("STATUS_OK"))
			return "ERROR_OCCURRED";

		return result;
	}

	@Override
	public String getFullSms(String apiKey, String code) {

		String result = template.getForObject(String.format(getFullSmsUrl + code, apiKey), String.class);

		if (result == null)
			return null;

		if ("BAD_KEY".equalsIgnoreCase(result))
			return "BAD_KEY";

		if ("STATUS_CANCEL".equalsIgnoreCase(result))
			return "CANCEL";

		if ("STATUS_WAIT_CODE".equalsIgnoreCase(result))
			return "WAITING";

		if ("NO_ACTIVATION".equalsIgnoreCase(result))
			return "NO_ACTIVATION";

		if (!result.toUpperCase().contains("FULL_SMS"))
			return "ERROR_OCCURRED";

		return result;
	}

	@Override
	public String cancel(String apiKey, String code) {
		System.out.println("cancel requested for code :: " + code);
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
			JSONObject jsonObject = new JSONObject(result).getJSONObject("22").getJSONObject(service);
			count = jsonObject.getString(jsonObject.keySet().stream().findFirst().get());
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
