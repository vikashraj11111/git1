package com.rk.otp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;

import com.rk.app.persistence.entity.CodeNumber;

public interface CodeNumberService {

	CodeNumber getByCode(String code);

	void saveAndFlush(CodeNumber codeNumber);

	Optional<CodeNumber> findById(String code);

	void deleteById(String code);

	boolean haveActiveNumber(Authentication authentication);
	
	int activeNumberCount(Authentication authentication);

	List<CodeNumber> findActiveNumberByAuthentication(Authentication authentication);
	
	List<CodeNumber> findActiveNumberByUsername(String username);

	CodeNumber getLatestActiveNumber(Authentication authentication);

	List<CodeNumber> getAllCodeNumbers();

	Optional<List<CodeNumber>> getAllAfter20MinNotUsed();

	Optional<List<CodeNumber>> getAllAfter25Min();

	Optional<List<CodeNumber>> getUsedCancelled();

	Optional<List<CodeNumber>> getAllNotUsed();

}
