package com.rk.otp.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.rk.app.persistence.entity.UsersOtpHistory;

public interface UsersOtpHistoryService {

	void saveHistory(UsersOtpHistory usersOtpHistory);

	List<UsersOtpHistory> getOtpHistoryByUsername(String username);

	Page<UsersOtpHistory> findPaginatedOtpHistory(Pageable pageable, String username);

	List<UsersOtpHistory> getOtpHistoryByCode(String code);

	void clearOtpHistory();

	void clearDuplicateOtpHistory();

}
