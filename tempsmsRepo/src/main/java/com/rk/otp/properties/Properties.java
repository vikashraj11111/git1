package com.rk.otp.properties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Properties {

	private List<String> serversList;
	
	private Map<String, Boolean> mailSentServerMap = new HashMap<>();


	public List<String> getServersList() {
		return serversList;
	}

	public void setServersList(List<String> serversList) {
		this.serversList = serversList;
	}
	
	public Map<String, Boolean> getMailSentServerMap() {
		if(mailSentServerMap.isEmpty())
			serversList.forEach(s -> mailSentServerMap.put(s, false));
		
		return mailSentServerMap;
	}

	public void putMailSentFlagInMap(String server, boolean mailSent) {
		mailSentServerMap.put(server, mailSent);
	}

}
