package com.rk.otp.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.rk.app.constants.CommonConstants;
import com.rk.app.exception.InvalidPasswordException;
import com.rk.app.exception.InvalidUsernameException;
import com.rk.app.exception.UserAlreadyExistsException;
import com.rk.app.mail.CustomMailSender;
import com.rk.app.persistence.entity.CodeNumber;
import com.rk.app.persistence.entity.Services;
import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.entity.VerificationToken;
import com.rk.app.persistence.entity.projection.UserUtility;
import com.rk.app.persistence.repository.CodeNumberRespository;
import com.rk.app.persistence.repository.ServiceRepository;
import com.rk.app.persistence.repository.UserRepository;
import com.rk.app.persistence.repository.VerificationTokenRepository;
import com.rk.app.user.UserDetailsImpl;
import com.rk.app.utility.Utility;
import com.rk.otp.service.UserBalanceAuditService;
import com.rk.otp.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import net.bytebuddy.utility.RandomString;

@Service
@Transactional
public class UserServiceImpl implements UserService {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ServiceRepository serviceRepository;

	@Autowired
	private CodeNumberRespository codeNumberRespository;

	@Autowired
	private UserBalanceAuditService userBalanceAuditService;

	@Autowired
	private CustomMailSender mailSender;

	@Autowired
	protected AuthenticationManager authenticationManager;

	@Autowired
	private VerificationTokenRepository verificationTokenRepository;

	private final List<User> usersList = new ArrayList<>();

	@Override
	public String register(User user, HttpServletRequest request)
			throws UserAlreadyExistsException, InvalidUsernameException, InvalidPasswordException {

		String tempPass = user.getPassword();
		user.setRole("ROLE_USER");
		createAccount(user);
		authWithAuthManager(request, user.getUsername(), tempPass);
//		sendVerificationEmail(user);

		return "Success";
	}

	@Override
	public void createAccount(User user) {
		if (!user.getUsername().matches(CommonConstants.USERNAME_REGEX)) {
			throw new InvalidUsernameException("Invalid username");
		}

		if (!user.getPassword().matches(CommonConstants.PASSWORD_REGEX)) {
			throw new InvalidPasswordException("Invalid password");
		}

		if (userRepo.findByUsernameIgnoreCase(user.getUsername()).orElse(null) != null) {
			LOGGER.error("User with username: {} already exists", user.getUsername());
			throw new UserAlreadyExistsException("User with username: " + user.getUsername() + " already exists");
		}

//		if (userRepo.findUserByEmail(user.getEmail()) != null) {
//			System.out.println("Email id already registerd: " + user.getEmail());
//			throw new EmailAlreadyExistsException(user.getEmail() + " already registered with different user");
//		}

		if (Utility.isValidPwd(user.getPassword()))
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		else
			user.setPassword(null);

//		String randomCode = RandomString.make(64);
//	    user.setVerificationCode(randomCode);

		userRepo.save(user);
	}

	public void authWithAuthManager(HttpServletRequest request, String username, String password) {
		try {
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
			authToken.setDetails(new WebAuthenticationDetails(request));

			Authentication authentication = authenticationManager.authenticate(authToken);

			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (Exception e) {
			LOGGER.error("Error occurred in authWithAuthManager: {}", e.getMessage(), e);
		}
	}

	@Override
	public void sendVerificationEmail(User user, String email, String hostName) {
		String randomCode = RandomString.make(64);

		VerificationToken token = new VerificationToken(randomCode, user);
		token.setEmail(email);
		verificationTokenRepository.save(token);

		String subject = "Please verify your email at tempsms";
		String content = "Dear [[name]],\n\n" + "Please click the link below to verify your email:\n\n" + "[[URL]] \n\n"
				+ "Thank you,\n" + "tempsms.";
		content = content.replace("[[name]]", user.getUsername());
		String verifyURL = "http://" + hostName + "/user/verify/" + randomCode;
		content = content.replace("[[URL]]", verifyURL);

		mailSender.sendVerificationMail(email, subject, content);
	}

	@Override
	public String sendPasswordResetEmail(String email, String hostName) {
		if (validateEmail(email) != "EXISTS") {
			return "NOT_EXISTS";
		}

		String randomCode = RandomString.make(64);
		User user = userRepo.findUserByEmail(email);
		VerificationToken token = new VerificationToken(randomCode, user);
		token.setEmail(email);
		verificationTokenRepository.save(token);

		String subject = "RESET PASSWORD | Tempsms";
		String content = "Dear [[name]],\n\n" + "Please click the link below to reset your password:\n\n"
				+ "[[URL]] \n\n" + "Thank you,\n" + "tempsms.";
		content = content.replace("[[name]]", user.getUsername());
		String verifyURL = "http://" + hostName + "/user/resetPassword/" + randomCode;
		content = content.replace("[[URL]]", verifyURL);

		mailSender.sendVerificationMail(email, subject, content);

		return "SENT";
	}

	@Override
	public String resendVerificationEmail(String username) {
		User user = userRepo.findByUsername(username).orElse(null);
		if (user == null)
			return "User Invalid";
		if (StringUtils.hasText(user.getEmail()))
			return "Email already exists";

		final VerificationToken token = verificationTokenRepository.findByUser(user);
		if (token == null) {
			return "Please Login and Add Email for better security";
		}
		String toAddress = token.getEmail();
		String subject = "Please verify your email at tempsms";
		String content = "Dear [[name]],\n\n" + "Please click the link below to verify your email:\n\n" + "[[URL]] \n\n"
				+ "Thank you,\n" + "tempsms.";
		content = content.replace("[[name]]", username);
		String verifyURL = "https://tempsms.store/user/verify/" + token.getToken();
		content = content.replace("[[URL]]", verifyURL);

		mailSender.sendVerificationMail(toAddress, subject, content);
		return "success";
	}

	@Override
	public boolean verify(String verificationCode) {
		final VerificationToken token = verificationTokenRepository.findByToken(verificationCode);
		if (token == null) {
			return false;
		}
		LOGGER.debug("Token :: {}", token);
		final User user = token.getUser();
		final Calendar cal = Calendar.getInstance();
		if ((token.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
			verificationTokenRepository.delete(token);
			return false;
		}
		LOGGER.debug("User :: {}", user);
		user.setEmail(token.getEmail());
		userRepo.save(user);

		verificationTokenRepository.delete(token);
		return true;
	}

	@Override
	public boolean verifyResetToken(String verificationCode) {
		final VerificationToken token = verificationTokenRepository.findByToken(verificationCode);
		if (token == null) {
			return false;
		}
		LOGGER.debug("Token :: {}", token);
		final Calendar cal = Calendar.getInstance();
		if ((token.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
			verificationTokenRepository.delete(token);
			return false;
		}

		return true;
	}

	@Override
	public User udpateUser(User user) {

		return userRepo.saveAndFlush(user);
	}

	@Override
	public boolean changePassword(User user) throws InvalidPasswordException {
		if (!user.getPassword().matches(CommonConstants.PASSWORD_REGEX))
			return false;

		User foundUser = userRepo.findByUsername(user.getUsername()).orElse(null);
		if (foundUser == null)
			return false;

		if (Utility.isValidPwd(user.getPassword()))
			foundUser.setPassword(passwordEncoder.encode(user.getPassword()));
		else
			return false;

		userRepo.saveAndFlush(foundUser);
		return true;
	}

	@Override
	public String changePassword(String currentPassword, String newPassword, Long userId) {

		if (currentPassword.equals(newPassword))
			return "New Password is same as current password";

		String validity = Utility.checkValidPassword(newPassword);

		if (!validity.equalsIgnoreCase("VALID"))
			return validity;

		User user = userRepo.getReferenceById(userId);

		if (!(Utility.checkValidCharacters(currentPassword)
				&& passwordEncoder.matches(currentPassword, user.getPassword())))
			return "Current Password is not valid";

		user.setPassword(passwordEncoder.encode(newPassword));
		userRepo.saveAndFlush(user);

		return "SUCCESS";
	}

	@Override
	public User getProjectedUser(Long id) {
		User user = null;
		Optional<User> optionalUser = userRepo.findById(id);
		if (optionalUser.isPresent())
			user = optionalUser.get();
		else
			LOGGER.error("Cant find User");

		return UserUtility.getUserWithNonSensitiveData(user);
	}

	@Override
	public Optional<User> getProjectedUserByUsername(String username) {
		User user = null;
		try {
			user = userRepo.findByUsername(username).orElse(null);
			if (user == null) {
				throw new NoSuchElementException("Cant find user");
			}
		} catch (NoSuchElementException e) {
			LOGGER.error("Cant find User");
		}
		return Optional.ofNullable(UserUtility.getUserWithNonSensitiveData(user));
	}

	@Override
	public User getUserBalance(String username) {
		return userRepo.findByUsername(username).orElse(null);
	}

	@Override
	public String validateUsername(String username) {
		return (userRepo.findByUsername(username).orElse(null) == null) ? "FAILED" : "SUCCESS";
	}

	@Override
	public Double getBalance(Long id) {
		Optional<User> optionalUser = userRepo.findById(id);
		if (optionalUser.isPresent()) {
			return optionalUser.get().getBalance();
		}
		return Double.valueOf(0);
	}

	@Override
	public User addBalance(String username, double amount, String byUser, String code) {
		User user = null;
		try {
			int i = userRepo.addBalance(amount, username);
			if (i == 1) {
				user = userRepo.findByUsername(username).orElse(null);
				user.setBalance(user.getBalance());
				userBalanceAuditService.audit(byUser, username, amount, "ADDED", user.getBalance(), code);
			}
		} catch (Exception ex) {
			LOGGER.error("Couln't update user : {}", ex.getMessage(), ex);
			return null;
		}
		return user;
	}

	@Override
	public boolean isBalanceAvailable(Authentication authentication, String service, String server) {

		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			Long id = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getId();
			double balance = getBalance(id);
//			TODO: call api and fetch amount based on server and service
			Double amount = serviceRepository.findById(service).get().getPrice();

			if (balance >= amount)
				return true;
		}
		return false;
	}

	@Override
	public List<CodeNumber> getAllNumbers(String user) {
//		Optional<List<CodeNumber>> allCodeNumberByUser = codeNumberRespository.getAllByUser(user);
		Optional<List<CodeNumber>> allCodeNumberByUser = codeNumberRespository.getAllByUserNotCancelled(user);

		if (allCodeNumberByUser.isEmpty())
			return null;

		List<CodeNumber> codeNumberList = allCodeNumberByUser.get();
		codeNumberList = codeNumberList.stream().filter(c -> {
			long diff = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Kolkata"))).getTime()
					- c.getCreatedTime().getTime();
			long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
			return seconds < 1200;
		}).collect(Collectors.toList());

		codeNumberList = codeNumberList.stream().map(c -> {
			Optional<Services> optionalService = serviceRepository.findServiceByCode(c.getService());
			String serviceName = c.getService();
			if (optionalService.isPresent())
				serviceName = optionalService.get().getServiceName();
			CodeNumber c1 = new CodeNumber(c);
			c1.setService(serviceName);
			return c1;
		}).collect(Collectors.toList());

		return codeNumberList;
	}

	@Override
	public User deductBalance(String username, double amount, String byUser, String code) {
		User user = null;
		try {
			int i = userRepo.deductBalance(amount, username);
			if (i == 1) {
				user = userRepo.findByUsername(username).orElse(null);
				user.setBalance(user.getBalance());
				if (user.getBalance() < (0 - amount)) {
					blockUser(username);
				}
				userBalanceAuditService.audit(byUser, username, amount, "DEDUCTED", user.getBalance(), code);
			}
		} catch (Exception ex) {
			LOGGER.error("Couln't update user : " + ex);
			return null;
		}
		return user;
	}

	@Override
	public void blockUser(String username) {
		userRepo.disableUser(username);

	}

	@Override
	public List<User> getAllUsers() {
		return userRepo.findAll();
	}

	public void initUserList(boolean refresh) {
		if (usersList.isEmpty()) {
			usersList.addAll(userRepo.findAllByOrderByBalanceDesc());
		} else if (refresh) {
			usersList.clear();
			usersList.addAll(userRepo.findAllByOrderByBalanceDesc());
		}
	}

	@Override
	public Page<User> findPaginatedUser(Pageable pageable, boolean refresh) {
		initUserList(refresh);
		int pageSize = pageable.getPageSize();
		int currentPage = pageable.getPageNumber();
		int startItem = currentPage * pageSize;
		List<User> list = usersList.stream().skip(startItem).limit(pageSize).collect(Collectors.toList());

		Page<User> userPage = new PageImpl<User>(list, PageRequest.of(currentPage, pageSize), usersList.size());

		return userPage;
	}

	@Override
	public String sumOfUsersBalance() {
		return String.valueOf(userRepo.findTotalBalance().orElse(null));
	}

	@Override
	public boolean refund(Authentication authentication, double price, String code) {
		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			String username = ((UserDetailsImpl) authentication.getPrincipal()).getUsername();
			int i = userRepo.addBalance(price, username);
			User user = new User();
			if (i == 1)
				user = userRepo.findByUsername(username).orElse(null);

			userBalanceAuditService.audit("SYSTEM", username, price, "REFUNDED", user.getBalance(), code);

			return true;
		}

		return false;
	}

	@Override
	public User refund(String username, double amount, String code) {
		User user = null;
		try {
			int i = userRepo.addBalance(amount, username);
			if (i == 1) {
				user = userRepo.findByUsername(username).orElse(null);
				user.setBalance(user.getBalance());
				userBalanceAuditService.audit("SYSTEM_TASK", username, amount, "REFUNDED", user.getBalance(), code);
			}
		} catch (Exception ex) {
			LOGGER.error("Couln't update user : " + ex);
			return null;
		}
		return user;
	}

	@Override
	public List<User> searchUserByUsernameKeyword(String keyword) {

		Optional<List<User>> result = userRepo.findFirst10ByUsernameContainingIgnoreCaseOrderByBalanceDesc(keyword);

		return result.orElse(null);
	}

	@Override
	public User getUser(String username) {

		return userRepo.findByUsername(username).orElse(null);
	}

	@Override
	public String validateEmail(String email) {

		return userRepo.findUserByEmail(email) != null ? "EXISTS" : "NOT_EXISTS";
	}

	@Override
	public void addOtpCount(String username) {
		Optional<User> optionalUser = userRepo.findByUsername(username);

		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			user.setOtpCount(user.getOtpCount() + 1);
			userRepo.saveAndFlush(user);
		}

	}

	@Override
	public boolean haveActiveNumbers(String user) {
		Optional<List<CodeNumber>> allByUserNotCancelled = codeNumberRespository.getAllByUserNotCancelled(user);
		return allByUserNotCancelled.isPresent() && !allByUserNotCancelled.get().isEmpty();
	}

	@Override
	public String resetPassword(String newPassword, String token) {
		String validity = Utility.checkValidPassword(newPassword);
		if (!validity.equalsIgnoreCase("VALID"))
			return validity;

		final VerificationToken verificationToken = verificationTokenRepository.findByToken(token);
		final User user = verificationToken.getUser();
		user.setPassword(passwordEncoder.encode(newPassword));
		userRepo.saveAndFlush(user);

		verificationTokenRepository.delete(verificationToken);

		return "SUCCESS";
	}

	@Override
	public String getActiveUsersBalance() {
		return String.valueOf(userRepo.findActiveUsersBalance().orElse(null));
	}

}
