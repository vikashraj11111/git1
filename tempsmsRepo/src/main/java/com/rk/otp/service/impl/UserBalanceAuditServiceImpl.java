package com.rk.otp.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.UserBalanceAudit;
import com.rk.app.persistence.entity.projection.AdminUserAmount;
import com.rk.app.persistence.repository.UserBalanceAuditRepository;
import com.rk.otp.service.UserBalanceAuditService;

@Service
public class UserBalanceAuditServiceImpl implements UserBalanceAuditService {

	@Autowired
	UserBalanceAuditRepository auditRepository;
	
	@Override
	public void audit(String byUser, String username, double amount, String msg, double updatedBalance, String refCode) {
		UserBalanceAudit userBalanceAudit = new UserBalanceAudit();
		userBalanceAudit.setAmount(amount);
		userBalanceAudit.setByUser(byUser);
		userBalanceAudit.setUser(username);
		userBalanceAudit.setMessage(msg);
		userBalanceAudit.setUpdatedBalance(updatedBalance);
		userBalanceAudit.setRefCode(refCode);
		
		auditRepository.saveAndFlush(userBalanceAudit);
		
	}
	
	@Override
	public List<AdminUserAmount> findTodaysAddedBalance(){
		return auditRepository.findTodaysEffectivelyAddedBalance().get();
	}
	
	@Override
	public List<UserBalanceAudit> findRechargeHistory(String username) {
		return auditRepository.findRechargeHistory(username).get();
	}
	
	@Override
	public void clearUserBalanceAudit() {
		auditRepository.clearUserBalanceAudit();
	}
	
}
