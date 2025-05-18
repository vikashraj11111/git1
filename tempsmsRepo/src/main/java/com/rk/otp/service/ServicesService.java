package com.rk.otp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rk.app.persistence.entity.Services;

public interface ServicesService {

	List<Services> getAllServices();

	String getServiceName(String code);

	Double getPrice(String code);

//	Map<String, List<Services>> getCategoryWiseServices();

	List<Services> getActiveServices();

	Optional<Services> findById(String service);

	Optional<String> findServiceCodeByServiceName(String serviceName);
	
	Optional<String> findServiceCodeByCode(String service);

	Page<Services> findPaginatedService(Pageable pageable, boolean refresh, String server);

	String getServerFromService(String service);
	
	boolean updateServicePrice(double price, String service);

	boolean addNewService(String server, String serviceCode, String serviceName, String code, Double price, String serviceId);

	List<Services> getAllServices(String server);
	
	List<String> getAllServers();

//	String copyServices(String fromServer, String toServer);
//	boolean copyService(Services service, String server);

}
