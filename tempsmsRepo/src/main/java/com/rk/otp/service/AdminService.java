package com.rk.otp.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface AdminService {

	Map<String, List<Double>> getActualCostOfServices(String server, double percentage);

	Map<String, Double> getActualCostOfServicesNew(String server, double perc);

	Map<String, Map<String, Double>> comparePriceAllServers(List<String> serversList);

	String backupFile(String table) throws IOException;

	ResponseEntity<?> restoreBackup(MultipartFile file);

}