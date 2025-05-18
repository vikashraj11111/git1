package com.rk.otp.service;

import java.util.List;

import com.rk.app.persistence.entity.ServerKeys;

public interface ServerKeysService {

	boolean update(String server, String updatedKey);

	String getServerKey(String server);
	
	boolean enableServer(String server);
	
	boolean disableServer(String server);
	
	List<ServerKeys> findAll();
	
	List<String> findAllServers();
	
	List<String> findAllEnabledServers();

}
