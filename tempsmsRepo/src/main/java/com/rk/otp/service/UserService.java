package com.rk.otp.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.rk.app.exception.InvalidPasswordException;
import com.rk.app.exception.UserAlreadyExistsException;
import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.User;

import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

	String register(User user, HttpServletRequest request) throws UserAlreadyExistsException;

	User getProjectedUser(Long id);

	Optional<User> getProjectedUserByUsername(String username);

	Double getBalance(Long id);

	User addBalance(String username, double amount, String byUser, String code);
	
	boolean isBalanceAvailable(Authentication authentication, String service, String server);

	List<CodeNumber> getAllNumbers(String user);

	User deductBalance(String username, double amount, String byUser, String code);

	User udpateUser(User user);

	User getUserBalance(String username);
	
	List<User> getAllUsers();

	Page<User> findPaginatedUser(Pageable pageable, boolean refresh);

	String sumOfUsersBalance();

	boolean refund(Authentication authentication, double price, String code);
	
	boolean changePassword(User user) throws InvalidPasswordException;

	List<User> searchUserByUsernameKeyword(String keyword);

	User getUser(String username);

	boolean verify(String verificationCode);

	String resendVerificationEmail(String username);

	void sendVerificationEmail(User user, String email, String hostName);

	String validateEmail(String email);

	User refund(String username, double amount, String code);

	String changePassword(String currentPassword, String newPassword, Long userId);

	void addOtpCount(String username);

	boolean haveActiveNumbers(String user);

	String validateUsername(String username);

	void blockUser(String username);

	String sendPasswordResetEmail(String email, String hostName);

	String resetPassword(String newPassword, String token);

	boolean verifyResetToken(String verificationCode);

	String getActiveUsersBalance();

	void createAccount(User user);

}
