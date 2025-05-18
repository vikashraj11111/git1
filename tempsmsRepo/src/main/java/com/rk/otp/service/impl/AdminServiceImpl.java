package com.rk.otp.service.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.rk.app.file.service.FileStorageService;
import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.ServerCostPerc;
import com.rk.app.persistence.entity.ServerKeys;
import com.rk.app.persistence.entity.ServiceList;
import com.rk.app.persistence.entity.Services;
import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.entity.UserBalanceAudit;
import com.rk.app.persistence.entity.UsersOtpHistory;
import com.rk.app.persistence.repository.ServiceListRepository;
import com.rk.otp.db.backup.service.BackupService;
import com.rk.otp.service.AdminService;
import com.rk.otp.service.OtpService;
import com.rk.otp.service.ServerCostPercService;
import com.rk.otp.service.ServicesService;

@Service
public class AdminServiceImpl implements AdminService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminServiceImpl.class);

	@Autowired
	private OtpService otpService;

	@Autowired
	private ServicesService servicesService;

	@Autowired
	private ServiceListRepository serviceListRepository;

	@Autowired
	private ServerCostPercService serverCostPercService;

	@Autowired
	private FileStorageService fileStorageService;

	@Autowired
	private BackupService backupService;

	@Autowired
	private CustomMailSender mailSender;

	@Override
	public Map<String, List<Double>> getActualCostOfServices(String server, double perc) {
		Map<String, List<Double>> servicePriceMap = new TreeMap<>();
		String result = otpService.getPriceAll(server);
		List<Services> allServices = servicesService.getAllServices(server);
		List<ServiceList> serviceList = serviceListRepository.findAll();
		if (result != null) {
			try {
				JSONObject jsonObject = new JSONObject(result).getJSONObject("22");
				jsonObject.keys().forEachRemaining(key -> {
					Double cost = Double.parseDouble(jsonObject.getJSONObject(key).getNumber("cost").toString());
					Double actualCost1 = cost * perc * 0.01;
					allServices.stream().anyMatch(service -> {
						if (service.getServiceCode().equalsIgnoreCase(key)) {
							servicePriceMap.put(service.getServiceName(),
									Arrays.asList(cost, actualCost1, service.getPrice()));
							return true;
						}
						return false;
					});

					serviceList.stream().anyMatch(s -> {
						if (s.getServiceCode().equalsIgnoreCase(key)) {
							servicePriceMap.putIfAbsent(s.getServiceName(), Arrays.asList(cost, actualCost1, 0.0));
							return true;
						}
						return false;
					});

				});
			} catch (JSONException e) {
				LOGGER.error("JsonException occurred : ", e);
			}
		}

		return servicePriceMap;
	}

	@Override
	public Map<String, Double> getActualCostOfServicesNew(String server, double perc) {
		Map<String, Double> servicePriceMap = new TreeMap<>();
		String result = otpService.getPriceAll(server);
		List<ServiceList> serviceList = serviceListRepository.findAll();
		if (result != null) {
			try {
				JSONObject jsonObject = new JSONObject(result).getJSONObject("22");
				serviceList.stream().forEach(service -> {
					if (jsonObject.keySet().stream().noneMatch(key -> {
						if (service.getServiceCode().equalsIgnoreCase(key)) {
							Double cost = Double
									.parseDouble(jsonObject.getJSONObject(key).getNumber("cost").toString());
							Double actualCost = cost * perc * 0.01;
							servicePriceMap.put(service.getServiceName(), actualCost);
							return true;
						}
						return false;
					}))
						servicePriceMap.put(service.getServiceName(), -1.0);
				});
			} catch (JSONException e) {
				return null;
			}
		}
		LOGGER.debug("getActualCostOfServicesNew() :: EXIT");
		return servicePriceMap;
	}

	@Override
	public Map<String, Map<String, Double>> comparePriceAllServers(List<String> serversList) {
		LOGGER.debug("comparePriceAllServers() :: ENTRY");
		Map<String, Map<String, Double>> servicePriceMap = new TreeMap<>();
		List<String> serversToRemove = new ArrayList<>();
		serversList.forEach(server -> {
			double percentage = serverCostPercService.getPercentage(server);
			Map<String, Double> map = getActualCostOfServicesNew(server, percentage);
			if (map != null)
				servicePriceMap.put(server, map);
			else
				serversToRemove.add(server);
		});
		serversList.removeAll(serversToRemove);
		LOGGER.debug("comparePriceAllServers() :: After First ForEach");
		Map<String, Map<String, Double>> servicePriceList = new HashMap<>();

		servicePriceMap.forEach((k, v) -> v.forEach((k1, v1) -> {
			if (!servicePriceList.containsKey(k1))
				servicePriceList.put(k1, new LinkedHashMap<>(Collections.singletonMap(k, v1)));
			else
				servicePriceList.get(k1).put(k, v1);
		})

		);

		LOGGER.debug("comparePriceAllServers() :: EXIT");
		return servicePriceList;
	}

	@Override
	public ResponseEntity<?> restoreBackup(MultipartFile file) {
		String fileName = fileStorageService.storeFile(file);

		String contentType = file.getContentType();
		if (!"text/csv".equals(contentType)) {
			return ResponseEntity.badRequest().body("Not a valid csv file");
		}

//		return ResponseEntity.ok(restoreService.restoreFromCsv(fileName));
		return ResponseEntity.ok(backupService.restoreBackup(fileName));
	}

	@Override
	public String backupFile(String table) throws IOException {
		String path = null;
		if (table.equalsIgnoreCase("user-details"))
			path = backupService.backupTable(User.class);
		else if (table.equalsIgnoreCase("service-list"))
			path = backupService.backupTable(ServiceList.class);
		else if (table.equalsIgnoreCase("services"))
			path = backupService.backupTable(Services.class);
		else if (table.equalsIgnoreCase("users-otp-history"))
			path = backupService.backupTable(UsersOtpHistory.class);
		else if (table.equalsIgnoreCase("server-keys"))
			path = backupService.backupTable(ServerKeys.class);
		else if (table.equalsIgnoreCase("server-cost-perc"))
			path = backupService.backupTable(ServerCostPerc.class);
		else if (table.equalsIgnoreCase("user-balance-audit"))
			path = backupService.backupTable(UserBalanceAudit.class);
		else if (table.equalsIgnoreCase("everything")) {
			CompletableFuture.runAsync(() -> {
				List<String> pathList = backupService.backupEverything();
				mailSender.sendBackupFiles("Backup files created - " + LocalDate.now(ZoneId.of("Asia/Kolkata")),
						pathList);
			});
		}

		if (path != null)
			mailSender.sendBackupFile(table + " - Backup file created - " + LocalDate.now(ZoneId.of("Asia/Kolkata")),
					path);
		return path;
	}

}
