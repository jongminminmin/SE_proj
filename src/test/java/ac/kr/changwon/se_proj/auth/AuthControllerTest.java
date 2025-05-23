package ac.kr.changwon.se_proj.auth;

import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 어노테이션 변경
@AutoConfigureMockMvc // MockMvc 자동 설정을 위해 추가
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean// AuthService는 컨트롤러 테스트를 위해 계속 Mock으로 사용
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;


    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequestDTO loginRequestDTO;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        Mockito.reset(authService);

        loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setUserId("testuser");
        loginRequestDTO.setPassword("password123"); // 이전 테스트에서는 "test", "1234"였으니 일관성 확인 필요

        // UserDto 초기화 (이전 테스트 케이스의 값과 일치시키거나, 테스트 시나리오에 맞게 조정)
        userDto = new UserDto("newuser", "New User Name", "newpassword", "newuser@example.com");
    }

    @Test
    @DisplayName("일반 로그인 성공 테스트")
    void login_success() throws Exception {
        given(authService.login(loginRequestDTO.getUserId(), loginRequestDTO.getPassword())).willReturn(true);

        ResultActions actions = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequestDTO)));

        actions.andExpect(status().isOk()) // AuthController는 성공/실패 모두 200 OK로 응답
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("일반 로그인 실패 테스트 - 잘못된 자격 증명")
    void login_failure_invalidCredentials() throws Exception {
        LoginRequestDTO loginDto = new LoginRequestDTO();
        String userId = "testuser"; // 명확성을 위해 변수 사용
        String wrongPassword = "password123asdasd";
        loginDto.setUserId(userId);
        loginDto.setPassword(wrongPassword);

        // *** 중요: authService.login이 BadCredentialsException을 던지도록 설정 ***
        given(authService.login(eq(userId), eq(wrongPassword)))
                .willThrow(new BadCredentialsException("비밀번호 불일치")); // 실제 예외 객체를 던지도록 변경

        ResultActions actions = mockMvc.perform(post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)));

        actions.andDo(print()) // 요청/응답 상세 정보 출력
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false)) // 이제 false를 기대
                .andExpect(jsonPath("$.message").value("비밀번호 불일치")); // AuthController의 catch 블록에서 설정한 메시지
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시도 시 실패 테스트") // UserAuthenticationIntegrationTest에서 가져온 경우
    void testLoginWithNonExistentUser() throws Exception {       // AuthControllerTest에 맞게 이름 변경 가능
        LoginRequestDTO loginDto = new LoginRequestDTO();
        String nonExistentUserId = "nonexistentuser";
        loginDto.setUserId(nonExistentUserId);
        loginDto.setPassword("somepassword");

        // *** 중요: authService.login이 UsernameNotFoundException을 던지도록 설정 ***
        given(authService.login(eq(nonExistentUserId), anyString())) // 비밀번호는 어떤 값이든 상관없음
                .willThrow(new UsernameNotFoundException("존재하지 않는 사용자 " + nonExistentUserId + " (from mock)"));

        ResultActions actions = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print());

        actions.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 사용자 " + nonExistentUserId + " (from mock)")); // AuthController의 catch 블록에서 설정한 메시지
        // 또는 .andExpect(jsonPath("$.message").value("존재하지 않는 사용자 " + nonExistentUserId + " (from mock)")); // 만약 e.getMessage()를 사용했다면
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void register_success() throws Exception {
        given(authService.register(userDto.getId(), userDto.getUsername(), userDto.getPassword(), userDto.getEmail()))
                .willReturn(true);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", userDto.getId());
        params.add("username", userDto.getUsername());
        params.add("password", userDto.getPassword());
        params.add("email", userDto.getEmail());

        ResultActions actions = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params));

        actions.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login")) // 회원가입 성공 시 /login으로 리다이렉트
                .andExpect(flash().attribute("registrationSuccess", "회원가입이 성공적으로 완료되었습니다. 로그인해주세요."));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이미 존재하는 아이디")
    void register_failure_alreadyExists() throws Exception {
        given(authService.register(userDto.getId(), userDto.getUsername(), userDto.getPassword(), userDto.getEmail()))
                .willReturn(false); // 예를 들어 ID나 이메일이 이미 존재

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("id", userDto.getId());
        params.add("username", userDto.getUsername());
        params.add("password", userDto.getPassword());
        params.add("email", userDto.getEmail());

        ResultActions actions = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .params(params));

        actions.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register")) // 회원가입 실패 시 /register로 리다이렉트
                .andExpect(flash().attribute("registrationError", "이미 존재하는 아이디이거나 회원가입에 실패했습니다. 다시 시도해주세요."));
    }
}