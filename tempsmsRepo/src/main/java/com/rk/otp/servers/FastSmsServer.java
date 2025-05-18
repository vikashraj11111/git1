package com.rk.otp.servers;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

@Service
@PropertySource(value = "classpath:servers/server4.properties")
public class FastSmsServer implements ServerApi {

	@Value("${fastsms.url.getBalance}")
	private String getBalanceUrl;

	@Value("${fastsms.url.service.price}")
	private String getPriceUrl;

	@Value("${fastsms.url.getnumber}")
	private String getNumberUrl;

	@Value("${fastsms.url.number.getstatus}")
	private String getStatusUrl;

	@Value("${fastsms.url.number.getfullsms}")
	private String getFullSmsUrl;

	@Value("${fastsms.url.number.cancel}")
	private String cancelUrl;

	@Value("${fastsms.url.getPriceAll}")
	private String getPriceAllUrl;

	@Value("${fastsms.url.resendOtp}")
	private String resendUrl;

	private final RestTemplate template;

	public FastSmsServer(RestTemplate template) {
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
		String cost = "";
		try {
			JSONObject jsonObject = new JSONObject(result).getJSONObject("22").getJSONObject(service);
			cost = jsonObject.keySet().stream().findFirst().get();
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
		
		if ("Api_blocked_for_5_seconds".equalsIgnoreCase(result))
			return "Api_blocked_for_5_seconds";

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

	@SuppressWarnings("unchecked")
	@Override
	public String getPriceAll(String apiKey) {
		String response = template.getForObject(String.format(getPriceAllUrl, apiKey), String.class);
		Map<String, Object> responseMap = new Gson().fromJson(response, Map.class);

		Map<Object, Object> collect = responseMap.entrySet().stream()
				.collect(
						Collectors
								.toMap(e -> e.getKey(),
										e -> ((Map<String, Object>) e.getValue()).entrySet().stream()
												.collect(
														Collectors.toMap(e2 -> e2.getKey(),
																e2 -> ((Map<String, Object>) e2.getValue()).entrySet()
																		.stream()
																		.flatMap(e3 -> Stream.of(
																				Map.entry("cost",
																						Double.parseDouble(
																								e3.getKey())),
																				Map.entry("count", e3.getValue())))
																		.collect(Collectors.toMap(Map.Entry::getKey,
																				Map.Entry::getValue))))));
		return new Gson().toJson(collect);
	}

	@Override
	public String resend(String apiKey, String code) {
		System.out.println(String.format(resendUrl + code, apiKey));
		String result = template.getForObject(String.format(resendUrl + code, apiKey), String.class);
		System.out.println("result :: " + result);
		if ("ACCESS_WAITING".equalsIgnoreCase(result) || "ACCESS_RETRY_GET".equalsIgnoreCase(result)
				|| "BAD_STATUS".equalsIgnoreCase(result)) {
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
