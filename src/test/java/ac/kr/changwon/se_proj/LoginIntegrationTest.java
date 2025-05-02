package java.ac.kr.changwon.se_proj;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;


@SpringBootTest
@AutoConfigureMockMvc
public class LoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** admin/1234 로 로그인 시도 → 인증 성공 (username = "admin") */
    @Test
    public void loginWithAdminShouldSucceed() throws Exception {
        mockMvc.perform(
                formLogin().
                        user("admin").
                        password("1234")
                        .with(csrf())
        )
                .andExpect(authenticated().withUsername("admin"));
    }

    /** user1/1234 로 로그인 시도 → 인증 성공 (username = "user1") */
    @Test
    public void loginWithUser1ShouldSucceed() throws Exception {
        mockMvc.perform(formLogin().user("user1").password("1234"))
                .andExpect(authenticated().withUsername("user1"));
    }

    /** admin/잘못된비밀번호 로 로그인 시도 → 인증 실패 */
    @Test
    public void loginWithWrongPasswordShouldFail() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("wrong"))
                .andExpect(unauthenticated());
    }

}
