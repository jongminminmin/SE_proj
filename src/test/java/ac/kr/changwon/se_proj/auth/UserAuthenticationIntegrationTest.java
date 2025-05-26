package ac.kr.changwon.se_proj.auth;


import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles
@Transactional
@Import(TestSecurityConfig.class)
public class UserAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserDto testUserDto;
    private LoginRequestDTO loginRequestDTO;

    @BeforeEach
    void setUp(){
        testUserDto = new UserDto("testuser", "Test User Name", "password123", "testuser@example.com");
        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUserId(testUserDto.getId());
        loginRequestDTO.setPassword(testUserDto.getPassword());
    }

    @Test
    @DisplayName("회원가입 성공 및 해당 정보로 로그인 성공 통합 테스트")
    void testRegistrationAndLogin() throws Exception{
        // 1. 회원가입 요청 (JSON으로 변경)
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON) // Content-Type을 JSON으로 설정
                        .content(objectMapper.writeValueAsString(testUserDto))) // testUserDto를 JSON 문자열로 변환하여 본문에 포함
                .andDo(print())
                .andExpect(status().isOk()) // AuthController.register가 성공 시 200 OK와 JSON을 반환한다고 가정
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다. 로그인해주세요."));

        // 2. DB에서 사용자 확인 (선택 사항, 이전과 동일)
        Optional<User> registeredUserOpt = userRepository.findById(testUserDto.getId());
        assertTrue(registeredUserOpt.isPresent(), "회원가입 후 DB에서 사용자를 찾을 수 있어야 합니다.");
        User registeredUser = registeredUserOpt.get();
        assertEquals(testUserDto.getUsername(), registeredUser.getUsername());
        assertTrue(passwordEncoder.matches(testUserDto.getPassword(), registeredUser.getPassword()), "저장된 비밀번호는 암호화되어야 합니다.");
        assertEquals("ROLE_USER", registeredUser.getRole(), "기본 역할은 ROLE_USER여야 합니다.");



        loginRequestDTO.setUserId(testUserDto.getId()); // loginRequestDTO의 ID를 방금 가입한 사용자로 설정
        loginRequestDTO.setPassword(testUserDto.getPassword()); // loginRequestDTO의 PW를 방금 가입한 사용자로 설정

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시도 시 실패 테스트")
    void testLoginWithNonExistentUser() throws Exception {
        LoginRequestDTO nonExistentUserLogin = new LoginRequestDTO();
        nonExistentUserLogin.setUserId("nonexistentuser");
        nonExistentUserLogin.setPassword("somepassword");

        ResultActions actions = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentUserLogin)))
                .andDo(print());

        actions.andExpect(status().isOk()) // HTTP 200 OK 기대
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false)); // success 필드는 false


        actions.andExpect(jsonPath("$.message").value("존재하지 않는 사용자 nonexistentuser"));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시도 시 실패 테스트")
    void testLoginWithInvalidPassword() throws Exception {
        // 1. 테스트 사용자 먼저 회원가입
        User userToRegister = new User(testUserDto.getId(), testUserDto.getUsername(), passwordEncoder.encode(testUserDto.getPassword()), testUserDto.getEmail(), "ROLE_USER");
        userRepository.save(userToRegister); // @Transactional로 인해 롤백되므로 다른 테스트에 영향 X

        // 2. 잘못된 비밀번호로 로그인 시도
        LoginRequestDTO wrongPasswordLogin = new LoginRequestDTO();
        wrongPasswordLogin.setUserId(testUserDto.getId());
        wrongPasswordLogin.setPassword("wrongpassword");

        ResultActions actions = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordLogin)))
                .andDo(print());

        // AuthController는 BadCredentialsException 발생 시 200 OK 와 success:false를 반환합니다.
        actions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("비밀번호 불일치")); // AuthController가 반환하는 메시지
    }

    @Test
    @DisplayName("이미 존재하는 ID로 회원가입 시도 시 실패 테스트")
    void testRegistrationWithExistingId() throws Exception {
        // 1. 테스트 사용자 먼저 회원가입 (이 부분은 실제 DB에 저장하는 방식이므로 그대로 두거나,
        //    API를 통해 가입시키는 방식으로 변경할 수도 있지만, 여기서는 userRepository를 직접 사용)
        User userToRegister = new User(testUserDto.getId(), "Original Name", passwordEncoder.encode(testUserDto.getPassword()), testUserDto.getEmail(), "ROLE_USER");
        userRepository.save(userToRegister);

        // 2. 동일한 ID로 다시 회원가입 시도 (JSON 요청으로 변경)
        // 중복 가입 시도에 사용할 DTO (기존 testUserDto 사용)
        UserDto duplicateUserDto = new UserDto(
                testUserDto.getId(), // 중복 ID
                "Another Name",
                "anotherpassword",
                "another@example.com"
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON) // Content-Type을 JSON으로 설정
                        .content(objectMapper.writeValueAsString(duplicateUserDto))) // duplicateUserDto를 JSON으로
                .andDo(print())
                .andExpect(status().isConflict()) // AuthController.register가 중복 시 409 Conflict 반환 가정
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디 또는 이메일 입니다.")); // 컨트롤러 반환 메시지에 따라
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 시 루트(/)로 리디렉션 통합 테스트")
    void testOauth2SuccessRedirect() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "OAuth2 Test User");
        attributes.put("email", "oauth2user@example.com");

        OAuth2UserAuthority authority = new OAuth2UserAuthority("ROLE_USER", attributes);
        OAuth2User mockOAuth2User = new DefaultOAuth2User(
                Collections.singleton(authority),
                attributes,
                "name" // nameAttributeKey
        );
        OAuth2AuthenticationToken mockAuthentication = new OAuth2AuthenticationToken(
                mockOAuth2User,
                Collections.emptyList(),
                "test-oauth2-client" // clientRegistrationId
        );

        mockMvc.perform(get("/oauth2/success")
                        .with(authentication(mockAuthentication))) // 시큐리티 컨텍스트에 인증 정보 제공
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
