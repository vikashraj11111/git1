package com.rk.otp.service;

import java.util.List;

import com.rk.app.persistence.entity.UserBalanceAudit;
import com.rk.app.persistence.entity.projection.AdminUserAmount;

public interface UserBalanceAuditService {

	void audit(String byUser, String username, double amount, String msg, double updatedBalance, String refCode);

	List<AdminUserAmount> findTodaysAddedBalance();

	List<UserBalanceAudit> findRechargeHistory(String username);

	void clearUserBalanceAudit();

}
