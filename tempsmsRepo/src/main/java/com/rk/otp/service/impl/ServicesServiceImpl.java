package com.rk.otp.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rk.app.persistence.entity.ServiceList;
import com.rk.app.persistence.entity.Services;
import com.rk.app.persistence.repository.ServiceListRepository;
import com.rk.app.persistence.repository.ServiceRepository;
import com.rk.otp.service.ServicesService;

@Service
@Transactional
public class ServicesServiceImpl implements ServicesService {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicesServiceImpl.class);

	@Autowired
	private ServiceRepository serviceRepository;
	
	@Autowired
	private ServiceListRepository serviceListRepository;
	
	private final List<Services> servicesList = new ArrayList<Services>();

	@Override
	public List<Services> getAllServices() {
		return serviceRepository.findAll();
	}
	
	@Override
	public List<Services> getAllServices(String server) {
		return serviceRepository.findServicesByServer(server);
	}
	
	@Override
	public List<Services> getActiveServices() {
		return serviceRepository.findActiveServices().orElse(null);
	}
	
	@Override
	public String getServiceName(String code) {
		Optional<Services> optionalService = serviceRepository.findById(code);
		if(optionalService.isEmpty()) {
			LOGGER.error("service not found with code : " + code);
			return "NOT_FOUND";
		}
		return optionalService.get().getServiceName();
	}
	
	@Override
	public Double getPrice(String code) {
		Optional<Services> optionalService = serviceRepository.findById(code);
		if(optionalService.isEmpty()) {
			LOGGER.error("service not found with code : " + code);
			return Double.valueOf(0);
		}
		return optionalService.get().getPrice();
	}
	
	public void initServiceList(boolean refresh) {
		if(servicesList.isEmpty()) {
			servicesList.addAll(serviceRepository.findAllByOrderByPriceDesc());
		} else if(refresh) {
			servicesList.clear();
			servicesList.addAll(serviceRepository.findAllByOrderByPriceDesc());
		}
	}
	
	@Override
	public Page<Services> findPaginatedService(Pageable pageable, boolean refresh, String server){
		initServiceList(refresh);
		int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<Services> list;
        List<Services> serviceList = getServiceList(server);
        
        list = serviceList.stream()
	        	.skip(startItem)
	        	.limit(pageSize)
	        	.collect(Collectors.toList());
        
		Page<Services> servicesPage = new PageImpl<Services>(list, PageRequest.of(currentPage, pageSize), serviceList.size());
        
        return servicesPage;
	}

	@Override
	public Optional<Services> findById(String service) {
		return serviceRepository.findById(service);
	}

	@Override
	public Optional<String> findServiceCodeByServiceName(String serviceName) {
		return serviceRepository.findServiceCodeByServiceName(serviceName);
	}
	
	@Override
	public Optional<String> findServiceCodeByCode(String service) {
		return serviceRepository.findServiceCodeByCode(service);
	}
	
	private List<Services> getServiceList(String server) {
		return servicesList.stream()
	        	.filter(service -> service.getServer().equalsIgnoreCase(server))
	        	.collect(Collectors.toList());
	}

	@Override
	public String getServerFromService(String service) {
		Optional<Services> optionalService = serviceRepository.findById(service);
		return optionalService.get().getServer();
	}

	@Override
	public boolean updateServicePrice(double price, String service) {
		if(serviceRepository.updateServicePrice(price, service) > 0)
			return true;
		return false;
	}
	

	@Override
	public boolean addNewService(String server, String serviceCode, String serviceName, String code, Double price, String serviceId) {
		
		try {
			ServiceList serviceList = serviceListRepository.findById(Integer.parseInt(serviceId)).orElse(null);
			if(serviceList == null)
				return false;
			
			Services services = new Services();
			
			services.setCode(code);
			services.setServiceName(serviceName);
			services.setServiceCode(serviceCode);
			services.setPrice(price);
			services.setServer(server);
			services.setServiceList(serviceList);
			
			serviceRepository.saveAndFlush(services);
		} catch (Exception e) {
			LOGGER.error("Exception occurred at addNewService : {}", e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	@Override
	public List<String> getAllServers() {
		return serviceRepository.findDistinctServers();
	}

//	@Override
//	public boolean copyService(Services service, String server) {
//		try {
//			System.out.println("Copy Started :: " + service);
//			String code = service.getCode();
//			String newCode = code.substring(0,code.length() - 1) + server.charAt(server.length()-1);
//			
//			Services newService = new Services(newCode, service.getServiceName(), service.getServiceId(), service.getPrice(), server);
//			
//			serviceRepository.saveAndFlush(newService);
//			System.out.println("Copy Done :: " + newService);
//		} catch (Exception e) {
//			System.out.println("Exception occurred at copyService for service : " + service + " :: " + e);
//			return false;
//		}
//		
//		return true;
//	}
//	
//	@Override
//	public String copyServices(String fromServer, String toServer) {
//		List<Services> servicesListToCopy = getAllServices(fromServer);
//		
//		servicesListToCopy.forEach(service -> copyService(service, toServer));
//		
//		return "COPY DONE";
//	}
	
}
