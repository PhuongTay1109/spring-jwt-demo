package com.tay.usermanager.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import com.tay.usermanager.service.UsersService;
import com.tay.usermanager.util.JWTUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JWTUtils jwtUtils;

	@Autowired
	private UsersService usersService;

	private static final Set<String> EXCLUDE_URL_PATTERNS = Stream.of("/auth/", "/public/").collect(Collectors.toSet());

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		if (ignore(request.getRequestURI())) {
			filterChain.doFilter(request, response);
			return;
		}

		final String jwt = resolveToken(request);
		final String refreshToken = resolveRefreshToken(request);

		// Request doesn't have jwt
		if (jwt == null || jwt.equals("null")) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
			return; // Stop further filter execution and return
		}

		try {
			String userEmail = jwtUtils.extractUsername(jwt);

			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				UserDetails userDetails = usersService.loadUserByUsername(userEmail);

				if (jwtUtils.isTokenValid(jwt, userDetails)) {
					UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails,
							null, userDetails.getAuthorities());
					token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(token);
				}
			}
		} catch (ExpiredJwtException e) {
			//SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token expired");
			return;
		}

		filterChain.doFilter(request, response);
	}

	// Extract "Bearer $token" from "Authorization" header
	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("authorization");

		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			String jwtString = bearerToken.substring(7);
			if (jwtString == null)
				return null;
			return bearerToken.substring(7);
		}

		return null;
	}

	private String resolveRefreshToken(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization-Refresh");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	private boolean ignore(String path) {
		return EXCLUDE_URL_PATTERNS.stream().anyMatch(path::startsWith);
	}
}
