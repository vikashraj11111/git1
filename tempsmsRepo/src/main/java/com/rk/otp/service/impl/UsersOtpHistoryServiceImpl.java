package com.rk.otp.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rk.app.persistence.entity.UsersOtpHistory;
import com.rk.app.persistence.repository.UsersOtpHistoryRespository;
import com.rk.otp.service.UsersOtpHistoryService;

@Service
@Transactional
public class UsersOtpHistoryServiceImpl implements UsersOtpHistoryService {

	@Autowired
	UsersOtpHistoryRespository otpHistoryRespository;
	
	@Override
	public void saveHistory(UsersOtpHistory usersOtpHistory) {
		
		otpHistoryRespository.saveAndFlush(usersOtpHistory);
	}
	
	@Override
	public List<UsersOtpHistory> getOtpHistoryByUsername(String username) {
		Optional<List<UsersOtpHistory>> optionalOtpHistoryList = otpHistoryRespository.findFirst100ByUsernameOrderByInsertTimeDesc(username);
		
		return optionalOtpHistoryList.orElse(null);
	}
	
	@Override
	public List<UsersOtpHistory> getOtpHistoryByCode(String code) {
		Optional<List<UsersOtpHistory>> optionalOtpHistoryList = otpHistoryRespository.findByCode(code);
		
		return optionalOtpHistoryList.orElse(null);
	}

	@Override
	public Page<UsersOtpHistory> findPaginatedOtpHistory(Pageable pageable, String username){
		int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        
        List<UsersOtpHistory> otpHistoryList = getOtpHistoryByUsername(username);
        List<UsersOtpHistory> list = otpHistoryList.stream()
								    	.skip(startItem)
								    	.limit(pageSize)
								    	.collect(Collectors.toList());
        
		Page<UsersOtpHistory> otpHistoryPage = new PageImpl<UsersOtpHistory>(list, PageRequest.of(currentPage, pageSize), otpHistoryList.size());
        
        return otpHistoryPage;
	}

	@Override
	public void clearOtpHistory() {
		otpHistoryRespository.clearOtpHistory();
	}

	@Override
	public void clearDuplicateOtpHistory() {
		otpHistoryRespository.clearDuplicateOtpHistory();
	}
	

}
