package com.example.quanghuy.account;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;

	public AuthService(AccountRepository accounts, PasswordEncoder passwordEncoder) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
	}

	public Account register(String rawUsername, String password) {
		String username = normalizeUsername(rawUsername);
		validatePassword(password);
		if (accounts.existsByUsername(username)) {
			throw new AuthException("Tên đăng nhập đã được sử dụng.");
		}
		try {
			return accounts.saveAndFlush(new Account(username, passwordEncoder.encode(password)));
		} catch (DataIntegrityViolationException exception) {
			throw new AuthException("Tên đăng nhập đã được sử dụng.");
		}
	}

	public Account login(String rawUsername, String password) {
		String username = normalizeUsername(rawUsername);
		Account account = accounts.findByUsername(username)
				.orElseThrow(() -> new AuthException("Tên đăng nhập hoặc mật khẩu không đúng."));
		if (password == null || !passwordEncoder.matches(password, account.getPasswordHash())) {
			throw new AuthException("Tên đăng nhập hoặc mật khẩu không đúng.");
		}
		return account;
	}

	public void changePassword(String rawUsername, String currentPassword, String newPassword) {
		Account account;
		try {
			account = login(rawUsername, currentPassword);
		} catch (AuthException exception) {
			throw new AuthException("Mật khẩu hiện tại không đúng.");
		}
		validatePassword(newPassword);
		if (passwordEncoder.matches(newPassword, account.getPasswordHash())) {
			throw new AuthException("Mật khẩu mới phải khác mật khẩu hiện tại.");
		}
		account.setPasswordHash(passwordEncoder.encode(newPassword));
		accounts.save(account);
	}

	private String normalizeUsername(String username) {
		if (username == null) {
			throw new AuthException("Tên đăng nhập không được để trống.");
		}
		String normalized = username.trim().toLowerCase(Locale.ROOT);
		if (!normalized.matches("[a-z0-9_]{3,30}")) {
			throw new AuthException("Tên đăng nhập cần có 3–30 ký tự: chữ thường, số hoặc dấu gạch dưới.");
		}
		return normalized;
	}

	private void validatePassword(String password) {
		if (password == null || password.length() < 8
				|| password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new AuthException("Mật khẩu cần có từ 8 đến 72 ký tự.");
		}
	}

	public static class AuthException extends RuntimeException {
		public AuthException(String message) {
			super(message);
		}
	}
}
