package com.example.quanghuy.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	AccountRepository accounts;

	@BeforeEach
	void clearAccounts() {
		accounts.deleteAll();
	}

	@Test
	void registerSavesHashedPassword() throws Exception {
		postJson("/api/auth/register", "{\"username\":\"Player_01\",\"password\":\"matkhau123\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value("player_01"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		Account saved = accounts.findByUsername("player_01").orElseThrow();
		assertThat(saved.getPasswordHash()).isNotEqualTo("matkhau123").startsWith("$2");
	}

	@Test
	void registerRejectsDuplicateUsername() throws Exception {
		register("player_01", "matkhau123");

		postJson("/api/auth/register", "{\"username\":\"PLAYER_01\",\"password\":\"matkhau999\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Tên đăng nhập đã được sử dụng."));
	}

	@Test
	void registerRejectsInvalidInput() throws Exception {
		postJson("/api/auth/register", "{\"username\":\"ab\",\"password\":\"matkhau123\"}")
				.andExpect(status().isBadRequest());
		postJson("/api/auth/register", "{\"username\":\"player_01\",\"password\":\"ngan\"}")
				.andExpect(status().isBadRequest());
		postJson("/api/auth/register", "{}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void loginWithCorrectAndWrongPassword() throws Exception {
		register("player_01", "matkhau123");

		postJson("/api/auth/login", "{\"username\":\"player_01\",\"password\":\"matkhau123\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("player_01"));

		postJson("/api/auth/login", "{\"username\":\"player_01\",\"password\":\"saimatkhau\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không đúng."));

		postJson("/api/auth/login", "{\"username\":\"khong_ton_tai\",\"password\":\"matkhau123\"}")
				.andExpect(status().isBadRequest());
	}

	@Test
	void changePasswordReplacesOldPassword() throws Exception {
		register("player_01", "matkhau123");

		postJson("/api/auth/change-password",
				"{\"username\":\"player_01\",\"currentPassword\":\"matkhau123\",\"newPassword\":\"matkhau456\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công."));

		postJson("/api/auth/login", "{\"username\":\"player_01\",\"password\":\"matkhau123\"}")
				.andExpect(status().isBadRequest());
		postJson("/api/auth/login", "{\"username\":\"player_01\",\"password\":\"matkhau456\"}")
				.andExpect(status().isOk());
	}

	@Test
	void changePasswordRequiresCurrentPassword() throws Exception {
		register("player_01", "matkhau123");

		postJson("/api/auth/change-password",
				"{\"username\":\"player_01\",\"currentPassword\":\"saimatkhau\",\"newPassword\":\"matkhau456\"}")
				.andExpect(status().isBadRequest());
		postJson("/api/auth/change-password",
				"{\"username\":\"player_01\",\"currentPassword\":\"matkhau123\",\"newPassword\":\"matkhau123\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Mật khẩu mới phải khác mật khẩu hiện tại."));
	}

	private void register(String username, String password) throws Exception {
		postJson("/api/auth/register",
				"{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
				.andExpect(status().isCreated());
	}

	private ResultActions postJson(String url, String body) throws Exception {
		return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
