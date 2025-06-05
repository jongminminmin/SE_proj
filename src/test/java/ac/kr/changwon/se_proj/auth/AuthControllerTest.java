package ac.kr.changwon.se_proj.auth;

import ac.kr.changwon.se_proj.config.TestSecurityConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 어노테이션 변경
@AutoConfigureMockMvc // MockMvc 자동 설정을 위해 추가
@Import(TestSecurityConfig.class)
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
    @DisplayName("존재하지 않는 사용자로 로그인 시도 시 실패 테스트")
    void testLoginWithNonExistentUser() throws Exception {
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
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void register_success() throws Exception{
        given(authService.register(eq(userDto.getId()), eq(userDto.getUsername()), eq(userDto.getPassword()), eq(userDto.getEmail())))
                .willReturn(true);

        ResultActions actions = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON) // <--- JSON으로 변경
                .content(objectMapper.writeValueAsString(userDto))); // <--- userDto를 JSON 문자열로 변환하여 본문에 포함

        // AuthController.register가 JSON 응답을 반환하도록 수정되었다고 가정
        actions.andDo(print())
                .andExpect(status().isOk()) // 또는 성공 시 201 Created 등
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 성공적으로 완료되었습니다. 로그인해주세요."));
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 이미 존재하는 아이디")
    void register_failure_alreadyExists() throws Exception {
        given(authService.register(eq(userDto.getId()), eq(userDto.getUsername()), eq(userDto.getPassword()), eq(userDto.getEmail())))
                .willReturn(false); // 서비스가 false 반환 (중복 등으로)

        ResultActions actions = mockMvc.perform(post("/api/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON) // <--- JSON으로 변경
                .content(objectMapper.writeValueAsString(userDto))); // <--- userDto를 JSON으로

        // AuthController.register가 ID/이메일 중복 시 409 Conflict와 JSON 응답을 반환한다고 가정
        actions.andDo(print())
                .andExpect(status().isConflict()) // HTTP 409 Conflict 기대
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 존재하는 아이디 또는 이메일 입니다.")); // 컨트롤러 반환 메시지에 따라
    }
}