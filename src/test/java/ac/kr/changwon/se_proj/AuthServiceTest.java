package ac.kr.changwon.se_proj;

import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {
    private AuthServiceImpl authService;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthServiceImpl(userRepository, passwordEncoder);
    }

    @Test
    public void testLoginWithEncryptedPassword() {
        // 비밀번호: 1234
        String encodedPassword = passwordEncoder.encode("1234");
        User testUser = new User("testuser", "nickname", encodedPassword, "test@example.com");

        when(userRepository.findById("testuser")).thenReturn(Optional.of(testUser));

        boolean result = authService.login("testuser", "1234");

        assertTrue(result, "비밀번호가 일치해야 로그인 성공");
    }

    @Test
    public void testLoginWithWrongPassword() {
        String encodedPassword = passwordEncoder.encode("1234");
        User testUser = new User("testuser", "nickname", encodedPassword, "test@example.com");

        when(userRepository.findById("testuser")).thenReturn(Optional.of(testUser));

        boolean result = authService.login("testuser", "wrongpassword");

        assertFalse(result, "잘못된 비밀번호는 로그인 실패");
    }

    @Test
    public void testLoginWithUnknownUser() {
        when(userRepository.findById("unknown")).thenReturn(Optional.empty());

        boolean result = authService.login("unknown", "any");

        assertFalse(result, "존재하지 않는 유저는 로그인 실패");
    }
}
