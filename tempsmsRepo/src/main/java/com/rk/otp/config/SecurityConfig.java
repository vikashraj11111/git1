package com.rk.otp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import com.rk.app.auth.handler.CustomAuthenticationFailureHandler;
import com.rk.app.auth.handler.CustomAuthenticationSuccessHandler;
import com.rk.app.user.service.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

	@Bean
	public AuthenticationSuccessHandler customAuthenticationSuccessHandler() {
		return new CustomAuthenticationSuccessHandler();
	}

	@Bean
	public AuthenticationFailureHandler customAuthenticationFailureHandler() {
		return new CustomAuthenticationFailureHandler();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, RememberMeServices rememberMeServices) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.requestMatchers("/admin/**", "/arekay/**")
						.hasAnyRole("ADMIN", "SUPERADMIN").requestMatchers("/sup/**").hasRole("SUPERADMIN")
						.requestMatchers("/login*", "/home**", "/register*", "/registration*", "/txt/**", "/css/**",
								"/js/**", "/images/**", "/public/**", "/", "/user/**", "/okay/**", "/favicon.ico")
						.permitAll()
//        			.requestMatchers("/register*", "/user/**", "/okay/**").hasRole("ADMIN")
//        			.requestMatchers("/login*", "/css/**", "/js/**", "/images/**", "/public/**", "/", "/favicon.ico").permitAll()
						.anyRequest().authenticated())
				.formLogin(form -> form.loginPage("/").loginProcessingUrl("/signin")
						.successHandler(customAuthenticationSuccessHandler())
						.failureHandler(customAuthenticationFailureHandler()))
				.logout(e -> e.logoutUrl("/logout").logoutSuccessHandler((req, res, auth) -> {
					req.getSession().setAttribute("message", "You are logged out successfully.");
					req.getSession().setAttribute("logoutSuccess", true);
					res.sendRedirect("/loginRedirect");
				}).deleteCookies("JSESSIONID", "SESSION").invalidateHttpSession(true))
				.exceptionHandling(e -> e.accessDeniedHandler((req, res, exp) -> res.sendRedirect("/accessDenied")))
//        		    .accessDeniedHandler((req, res, exp) -> res.sendRedirect("/public/maintenance")))
//        	.rememberMe(rem -> rem
//        			.rememberMeCookieName("tempsms")
//        		    .key("tempsmskey"))
				.rememberMe((remember) -> remember.rememberMeServices(rememberMeServices))
				.sessionManagement((sessionManagement) -> sessionManagement.sessionConcurrency(
						(sessionConcurrency) -> sessionConcurrency.maximumSessions(1).maxSessionsPreventsLogin(false)
								.expiredUrl("/loginRedirect").expiredSessionStrategy(e -> {
									String errMsg = "Logged in from different system/browser. Only 1 device permitted";
									e.getRequest().getSession().setAttribute("message", errMsg);
									e.getResponse().sendRedirect("/loginRedirect");
								}))
//    								.invalidSessionUrl("/")
						.invalidSessionStrategy((req, res) -> {
							String errMsg = "Session has Expired. Please Login again";
							req.getSession().setAttribute("message", errMsg);
							res.sendRedirect("/loginRedirect");
						}).sessionAuthenticationFailureHandler((req, res, exp) -> {
							String errMsg = "Password has been changed. Login with new password";
							req.getSession().setAttribute("message", errMsg);
							res.sendRedirect("/loginRedirect");
						}).sessionFixation().changeSessionId());
		return http.build();
	}

	@Bean
	public RememberMeServices rememberMeServices(UserDetailsService userDetailsService) {
		return new TokenBasedRememberMeServices("tempsmskey", userDetailsService);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public UserDetailsServiceImpl userDetailsService() {
		return new UserDetailsServiceImpl();
	}

	@Bean
	public AuthenticationManager authenticationManager() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService());
		authProvider.setPasswordEncoder(passwordEncoder());
		return new ProviderManager(authProvider);
	}

}
