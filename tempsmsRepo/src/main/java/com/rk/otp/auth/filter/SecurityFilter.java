package com.rk.otp.auth.filter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.rk.app.mail.CustomMailSender;
import com.rk.app.user.UserDetailsImpl;
import com.rk.otp.service.OtpService;
import com.rk.otp.service.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

	@Autowired
	private UserService userService;

	@Autowired
	private CustomMailSender mailSender;

	@Autowired
	private OtpService otpService;

	private int MAX_REQUESTS_PER_SECOND = 10;
	private int MAX_TRANSACTIONAL_PER_HOUR = 5;

	private LoadingCache<String, Integer> requestCountsPerIpAddress;
	private LoadingCache<String, Integer> transactionalRequestCountsPerIpAddress;

	public SecurityFilter() {
		requestCountsPerIpAddress = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});

		transactionalRequestCountsPerIpAddress = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
				.build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = null;

		if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
			if (authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
					.anyMatch((a) -> a.toUpperCase().contains("ADMIN"))) {
				filterChain.doFilter(request, response);
				return;
			}
			username = ((UserDetailsImpl) authentication.getPrincipal()).getUser().getUsername();
		}

		String requestURI = request.getRequestURI();
		if (requestURI.startsWith("/user/recharge") && isMaximumRequestsPerSecondExceeded(getClientIP(request))) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.getWriter().write("Bhag Bhosdike");
			if (username != null)
				userService.blockUser(username);
			else
				return;
		} else if ((requestURI.startsWith("/user/registration") || requestURI.startsWith("/public/contactus"))
				&& isMaximumRequestsPerHourExceeded(getClientIP(request))) {
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.getWriter().write("Bhag Bhosdike");
			return;
		}

		if (username != null && !userService.getUser(username).getEnabled()) {
			doLogout(request);
			otpService.cancelNumbers(username);
			mailSender.sendEmailToAdminHtml("Fraud Found - " + username,
					"The account associated with username <b>" + username + "</b> has been blocked and is logged out "
							+ "beacuse it was found to be causing harm to the system.");
			return;
		}

		String referrer = request.getHeader("referer");
		String queryString = request.getQueryString();
		String method = request.getMethod();

		if (!"GET".equals(method) || requestURI.startsWith("/okay/v1")) {
			filterChain.doFilter(request, response);
		} else if ((queryString != null && queryString.startsWith("loadUrl")) || (requestURI.contains(".")
				|| requestURI.equals("/") || (referrer != null && referrer.contains("/home")))) {
			request.getSession().removeAttribute("urlPath");
			filterChain.doFilter(request, response);
		} else {
			request.getSession().setAttribute("urlPath", requestURI);
			request.getRequestDispatcher("/home").forward(request, response);
		}

	}

	private void doLogout(HttpServletRequest request) {
		try {
			request.logout();
			String errMsg = "Account blocked due to suspicious activity";
			request.getSession().setAttribute("message", errMsg);
		} catch (ServletException e) {
			e.printStackTrace();
		}
	}

	private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress) {
		Integer requests = 0;
		requests = requestCountsPerIpAddress.get(clientIpAddress);
		if (requests != null) {
			if (requests > MAX_REQUESTS_PER_SECOND) {
				requestCountsPerIpAddress.asMap().remove(clientIpAddress);
				requestCountsPerIpAddress.put(clientIpAddress, requests);
				return true;
			}

		} else {
			requests = 0;
		}
		requests++;
		requestCountsPerIpAddress.put(clientIpAddress, requests);
		return false;
	}

	private boolean isMaximumRequestsPerHourExceeded(String clientIpAddress) {
		Integer requests = 0;
		requests = transactionalRequestCountsPerIpAddress.get(clientIpAddress);
		if (requests != null) {
			if (requests > MAX_TRANSACTIONAL_PER_HOUR) {
				transactionalRequestCountsPerIpAddress.asMap().remove(clientIpAddress);
				transactionalRequestCountsPerIpAddress.put(clientIpAddress, requests);
				return true;
			}

		} else {
			requests = 0;
		}
		requests++;
		transactionalRequestCountsPerIpAddress.put(clientIpAddress, requests);
		return false;
	}

	public String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}

}
