package com.rk.otp.servers;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@PropertySource(value = "classpath:servers/server5.properties", ignoreResourceNotFound = true)
public class GrizzlySmsServer implements ServerApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(GrizzlySmsServer.class);

	@Value("${grizzlysms.url.getBalance}")
	private String getBalanceUrl;

	@Value("${grizzlysms.url.service.price}")
	private String getPriceUrl;

	@Value("${grizzlysms.url.getnumber}")
	private String getNumberUrl;

	@Value("${grizzlysms.url.number.getstatus}")
	private String getStatusUrl;

	@Value("${grizzlysms.url.number.getfullsms}")
	private String getFullSmsUrl;

	@Value("${grizzlysms.url.number.cancel}")
	private String cancelUrl;

	@Value("${grizzlysms.url.getPriceAll}")
	private String getPriceAllUrl;
	
	private final RestTemplate template;
	
	public GrizzlySmsServer(RestTemplate template) {
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
			JSONObject jsonObject = new JSONObject(result);
			cost = jsonObject.getJSONObject("22").getJSONObject(service).getNumber("cost").toString();
		} catch (JSONException e) {
			LOGGER.error("getPrice :: JsonException occurred: ", e);
		}

		return cost;
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

		return template.getForObject(String.format(cancelUrl + code, apiKey), String.class);
	}

	@Override
	public String getPriceAll(String apiKey) {

		return template.getForObject(String.format(getPriceAllUrl, apiKey), String.class);
	}

	@Override
	public String getCount(String apiKey, String service) {
		String result = template.getForObject(String.format(getPriceUrl + service, apiKey), String.class);

		String count = "";
		try {
			JSONObject jsonObject = new JSONObject(result);
			count = jsonObject.getJSONObject("22").getJSONObject(service).get("count").toString();
		} catch (JSONException e) {
			LOGGER.error("getCount :: JsonException occurred: ", e);
		}

		return count;
	}

	@Override
	public String resend(String apiKey, String code) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getNumberByCountry(String apiKey, String service, String country) {
		// TODO Auto-generated method stub
		return null;
	}

}
