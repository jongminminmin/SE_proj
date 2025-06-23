package ac.kr.changwon.se_proj.controller.login;

import ac.kr.changwon.se_proj.config.TestSecurityConfig; // 테스트 설정 Import
import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import; // Import 어노테이션
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class) // 테스트 전용 SecurityConfig를 불러옵니다.
@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserDetailsService userDetailsService;

    // TestSecurityConfig에서 PasswordEncoder를 Bean으로 등록하므로, @MockBean은 필요 없습니다.

    @Nested
    @DisplayName("아이디/비밀번호 찾기")
    class FindCredentialTests {

        @Test
        @DisplayName("비밀번호 찾기 / 입력: 잘못된 정보 / 기대 결과: 404 Not Found")
        void verifyUserForPasswordReset_fail_withWrongInfo() throws Exception {
            // given
            FindPasswordRequestDto requestDto = new FindPasswordRequestDto();
            requestDto.setUserId("wronguser");
            requestDto.setEmail("wrong@test.com");

            given(authService.verifyUserForPasswordReset(any(FindPasswordRequestDto.class))).willReturn(false);

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/find/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto))); // .with(csrf()) 제거
            // then
            // TestSecurityConfig에 의해 /api/auth/** 가 허용되었으므로,
            // 더 이상 로그인 페이지로 리디렉션(302)되지 않고 컨트롤러 로직이 정상 수행됩니다.
            resultActions
                    .andExpect(status().isNotFound()) // 404 상태 코드를 기대
                    .andExpect(jsonPath("$.success").value(false))
                    .andDo(print());
        }

        @Test
        @DisplayName("비밀번호 찾기 / 입력: 올바른 아이디/이메일 / 기대 결과: 200 OK")
        void verifyUserForPasswordReset_success_withCorrectInfo() throws Exception {
            // given
            FindPasswordRequestDto requestDto = new FindPasswordRequestDto();
            requestDto.setUserId("testuser");
            requestDto.setEmail("test@test.com");

            given(authService.verifyUserForPasswordReset(any(FindPasswordRequestDto.class))).willReturn(true);

            // when
            ResultActions resultActions = mockMvc.perform(post("/api/auth/find/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestDto))); // .with(csrf()) 제거

            // then
            resultActions
                    .andExpect(status().isOk()) // 200 상태 코드를 기대
                    .andExpect(jsonPath("$.success").value(true))
                    .andDo(print());
        }
    }
}
