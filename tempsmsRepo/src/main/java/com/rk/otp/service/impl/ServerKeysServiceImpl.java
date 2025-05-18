package com.rk.otp.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rk.app.persistence.entity.ServerKeys;
import com.rk.app.persistence.repository.ServerKeysRepository;
import com.rk.otp.service.ServerKeysService;

@Service
@Transactional
public class ServerKeysServiceImpl implements ServerKeysService {

	@Autowired
	private ServerKeysRepository serverKeysRepository;
	
	@Override
	public boolean update(String server, String updatedKey) {
		Optional<ServerKeys> serverKey = serverKeysRepository.findById(server);
		serverKey.ifPresent(s ->  {
			s.setApiKey(updatedKey);
			serverKeysRepository.saveAndFlush(s);
		});
		
		return true;
	}
	
	@Override
	public String getServerKey(String server) {
		return serverKeysRepository.getReferenceById(server).getApiKey();
	}

	@Override
	public boolean enableServer(String server) {
		return serverKeysRepository.enableDisableServer(server, true) > 0;
	}

	@Override
	public boolean disableServer(String server) {
		return serverKeysRepository.enableDisableServer(server, false) > 0;
	}

	@Override
	public List<ServerKeys> findAll() {
		return serverKeysRepository.findAll();
	}

	@Override
	public List<String> findAllEnabledServers() {
		return serverKeysRepository.findAllEnabledServers();
	}

	@Override
	public List<String> findAllServers() {
		return serverKeysRepository.findAllServers();
	}
	
}
