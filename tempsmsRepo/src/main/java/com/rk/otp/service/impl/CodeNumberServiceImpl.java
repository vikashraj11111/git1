package com.rk.otp.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.repository.CodeNumberRespository;
import com.rk.app.user.UserDetailsImpl;
import com.rk.otp.service.CodeNumberService;
import com.rk.otp.service.UserBalanceAuditService;
import com.rk.otp.service.UserService;

@Service
public class CodeNumberServiceImpl implements CodeNumberService {

	@Autowired
	UserService userService;

	@Autowired
	CodeNumberRespository codeNumberRespository;
	
	@Autowired
	UserBalanceAuditService userBalanceAuditService;

	@Override
	public CodeNumber getByCode(String code) {
		return codeNumberRespository.findById(code).orElse(null);
	}

	@Override
	public void saveAndFlush(CodeNumber codeNumber) {
		
		codeNumberRespository.saveAndFlush(codeNumber);
	}

	@Override
	public Optional<CodeNumber> findById(String code) {
		
		return codeNumberRespository.findById(code);
	}

	@Override
	public void deleteById(String code) {
		
		codeNumberRespository.deleteById(code);
	}
	
	@Override
	public boolean haveActiveNumber(Authentication authentication) {
		
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			Optional<List<CodeNumber>> allByUserNotCancelled = codeNumberRespository.getAllByUserNotCancelled(username);
			if(allByUserNotCancelled.isPresent()) {
				return !allByUserNotCancelled.get().isEmpty();
			}
		}
		
		return false;
	}
	
	@Override
	public int activeNumberCount(Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			Optional<List<CodeNumber>> allByUserNotCancelled = codeNumberRespository.getAllByUserNotCancelled(username);
			if(allByUserNotCancelled.isPresent()) {
				return allByUserNotCancelled.get().size();
			}
		}
		
		return 0;
	}
	
	@Override
	public List<CodeNumber> findActiveNumberByAuthentication(Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			Optional<List<CodeNumber>> allByUserNotCancelled = codeNumberRespository.getAllByUserNotCancelled(username);
			if(allByUserNotCancelled.isPresent()) {
				return allByUserNotCancelled.get();
			}
		}
		
		return null;
	}
	
	@Override
	public List<CodeNumber> findActiveNumberByUsername(String username) {
		return codeNumberRespository.getAllByUserNotCancelled(username).get();
	}
	
	@Override
	public CodeNumber getLatestActiveNumber(Authentication authentication) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			Optional<List<CodeNumber>> allByUserNotCancelled = codeNumberRespository.getAllByUserNotCancelled(username);
			if(allByUserNotCancelled.isPresent()) {
				return allByUserNotCancelled.get().get(0);
			}
		}
		
		return null;
	}
	
	@Override
	public List<CodeNumber> getAllCodeNumbers() {
		
		return codeNumberRespository.findAllByOrderByOtpReceivedCreatedTimeDesc();
	}

	@Override
	public Optional<List<CodeNumber>> getAllAfter20MinNotUsed() {
		return codeNumberRespository.getAllAfter20MinNotUsed();
	}

	@Override
	public Optional<List<CodeNumber>> getAllAfter25Min() {
		return codeNumberRespository.getAllAfter25Min();
	}

	@Override
	public Optional<List<CodeNumber>> getUsedCancelled() {
		return codeNumberRespository.getUsedCancelled();
	}

	@Override
	public Optional<List<CodeNumber>> getAllNotUsed() {
		return codeNumberRespository.getAllNotUsed();
	}

}