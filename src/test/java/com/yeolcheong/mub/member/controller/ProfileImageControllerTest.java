package com.yeolcheong.mub.member.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yeolcheong.mub.member.config.SecurityConfig;
import com.yeolcheong.mub.member.dto.ProfileImageResponse;
import com.yeolcheong.mub.member.security.JwtAuthenticationFilter;
import com.yeolcheong.mub.member.security.JwtTokenProvider;
import com.yeolcheong.mub.member.service.ProfileImageService;

@WebMvcTest(controllers = ProfileImageController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = {
	"app.image.upload-dir=images",
	"app.image.profile-dir=profile",
	"app.image.base-url=http://localhost:8083",
	"app.image.url-path-prefix=/images",
	"app.image.default-profile-path=profile/default.png"
})
@DisplayName("ProfileImageController slice tests")
class ProfileImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfileImageService profileImageService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@Test
	@DisplayName("POST /api/members/me/profile-image - returns 201 with image url on success")
	@WithMockUser(username = "1")
	void registerReturns201() throws Exception {
		given(profileImageService.register(eq(1L), any()))
			.willReturn(ProfileImageResponse.of("http://localhost:8083/images/profile/uuid.png", false));

		mockMvc.perform(multipart("/api/members/me/profile-image")
				.file(new MockMultipartFile("file", "x.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1, 2, 3})))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.imageUrl").value("http://localhost:8083/images/profile/uuid.png"))
			.andExpect(jsonPath("$.default").value(false));
	}

	@Test
	@DisplayName("POST /api/members/me/profile-image - returns 403 when unauthenticated")
	void registerReturns403WhenUnauthenticated() throws Exception {
		mockMvc.perform(multipart("/api/members/me/profile-image")
				.file(new MockMultipartFile("file", "x.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1, 2, 3})))
			.andDo(print())
			.andExpect(status().isForbidden());

		then(profileImageService).should(never()).register(anyLong(), any());
	}

	@Test
	@DisplayName("PUT /api/members/me/profile-image - returns 200 with updated image url")
	@WithMockUser(username = "1")
	void updateReturns200() throws Exception {
		given(profileImageService.update(eq(1L), any()))
			.willReturn(ProfileImageResponse.of("http://localhost:8083/images/profile/new.png", false));

		mockMvc.perform(multipart("/api/members/me/profile-image")
				.file(new MockMultipartFile("file", "new.png", MediaType.IMAGE_PNG_VALUE, new byte[] {4, 5}))
				.with(request -> {
					request.setMethod("PUT");
					return request;
				}))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl").value("http://localhost:8083/images/profile/new.png"));
	}

	@Test
	@DisplayName("DELETE /api/members/me/profile-image - returns 200 with default image")
	@WithMockUser(username = "1")
	void deleteReturnsDefault() throws Exception {
		given(profileImageService.delete(1L))
			.willReturn(ProfileImageResponse.of("http://localhost:8083/images/profile/default.png", true));

		mockMvc.perform(delete("/api/members/me/profile-image"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.default").value(true));
	}

	@Test
	@DisplayName("GET /api/members/me/profile-image - returns 200 with current image url")
	@WithMockUser(username = "1")
	void getReturns200() throws Exception {
		given(profileImageService.get(1L))
			.willReturn(ProfileImageResponse.of("http://localhost:8083/images/profile/uuid.png", false));

		mockMvc.perform(get("/api/members/me/profile-image"))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.imageUrl").value("http://localhost:8083/images/profile/uuid.png"));
	}
}
