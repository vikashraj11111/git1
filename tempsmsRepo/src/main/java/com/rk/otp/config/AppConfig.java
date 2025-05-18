package com.rk.otp.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.rk.app.persistence.entity.MetaData;
import com.rk.app.persistence.entity.User;
import com.rk.app.persistence.repository.MetaDataRepo;
import com.rk.app.persistence.repository.UserRepository;
import com.rk.otp.constants.AppConstants;
import com.rk.otp.properties.Properties;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

@Configuration
@EnableEncryptableProperties
public class AppConfig {

	@Value("${spring.mail.username}")
	private String fromEmail;
	
	@Value("${bharatpe.validate.url}")
	private String bharatpeUrl;
	
	@Value("${bharatpe.upi.id}")
	private String bharatpeUpiId;

	@Autowired
	private MetaDataRepo metaDataRepo;

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean("template")
	RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(20))
				.build();
	}

	@Bean("webCient")
	WebClient webClient() {
		return WebClient.builder().build();
	}

	@Bean("simpleMailMessage")
	SimpleMailMessage simpleMailMessage() {
		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(fromEmail);
		return msg;
	}

	@Bean("properties")
	Properties initProperties() {
		final Properties properties = new Properties();
		List<String> serversList = new ArrayList<>();
		serversList.add("server1");
		serversList.add("server2");
		serversList.add("server3");
		serversList.add("server4");
		serversList.add("server5");
		serversList.add("server6");
		properties.setServersList(serversList);

		return properties;
	}

	@Bean
	String insertMandatoryEntries() {
		insertAdminData("admin", "T3mp5m5", "admin@tempsms.store");
		insertMetadata("isRestoreEnabled", AppConstants.FALSE);
		insertMetadata("mini.store.url", "https://tempsms.mini.site");
		insertMetadata("recharge.enabled.crypto", AppConstants.FALSE);
		insertMetadata("recharge.enabled.upi", "true");
		insertMetadata("recharge.enabled.bharatpe", AppConstants.FALSE);
		insertMetadata("bharatpe.merchant.token", "");
		insertMetadata("recharge.enabled.paytm", AppConstants.FALSE);
		insertMetadata("paytm.merchant.id", "");
		insertMetadata("bharatpe.upi.id", bharatpeUpiId);
		return "success";
	}

	private void insertMetadata(String attribute, String value) {
		List<MetaData> metaDataList = metaDataRepo.findAll();
		if (metaDataList.stream().noneMatch(m -> m.getAttribute().equalsIgnoreCase(attribute))) {
			OptionalLong optionalLastId = metaDataList.stream().mapToLong(m -> m.getId()).max();
			Long id = optionalLastId.isPresent() ? optionalLastId.getAsLong() + 1 : 1;
			MetaData metaData = new MetaData(id, attribute, value);
			metaDataRepo.saveAndFlush(metaData);
		}

	}

	private void insertAdminData(String username, String defaultPassword, String email) {
		 Optional<User> optionalUser = userRepository.findByUsername(username);
		 if(optionalUser.isEmpty()) {
			 User user = new User();
			 user.setUsername(username);
			 user.setPassword(passwordEncoder.encode(defaultPassword));
			 user.setRole("ROLE_ADMIN");
			 user.setEmail(email);
			 userRepository.save(user);
		 }

	}

}
