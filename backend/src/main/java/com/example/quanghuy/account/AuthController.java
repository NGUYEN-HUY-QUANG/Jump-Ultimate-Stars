package com.example.quanghuy.account;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public ResponseEntity<?> register(@RequestBody Credentials request) {
		Account account = authService.register(request.username(), request.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
	}

	@PostMapping("/login")
	public AccountResponse login(@RequestBody Credentials request) {
		return AccountResponse.from(authService.login(request.username(), request.password()));
	}

	@PostMapping("/change-password")
	public Map<String, String> changePassword(@RequestBody ChangePasswordRequest request) {
		authService.changePassword(request.username(), request.currentPassword(), request.newPassword());
		return Map.of("message", "Đổi mật khẩu thành công.");
	}

	@ExceptionHandler(AuthService.AuthException.class)
	public ResponseEntity<Map<String, String>> handleAuthError(AuthService.AuthException exception) {
		return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
	}

	public record Credentials(String username, String password) {}
	public record ChangePasswordRequest(String username, String currentPassword, String newPassword) {}
	public record AccountResponse(Long id, String username, Instant createdAt) {
		static AccountResponse from(Account account) {
			return new AccountResponse(account.getId(), account.getUsername(), account.getCreatedAt());
		}
	}
}
