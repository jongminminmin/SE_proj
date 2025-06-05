package ac.kr.changwon.se_proj.auth;

import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class) // 순수 Mockito 테스트를 위함
@Import(TestSecurityConfig.class)
public class AuthServiceTest {

    @Mock // UserRepository는 의존성이므로 @Mock으로 선언
    private UserRepository userRepository;

    @Mock // PasswordEncoder도 의존성이므로 @Mock으로 선언
    private PasswordEncoder passwordEncoder;

    @InjectMocks // 테스트 대상인 AuthServiceImpl 인스턴스를 생성하고 위 @Mock 객체들을 주입
    private AuthServiceImpl authService; // 타입을 AuthServiceImpl로 변경하고 @InjectMocks 사용

    private User testUser;
    private String rawPassword = "password123";
    private String encodedPassword = "encodedPassword123";


    @BeforeEach
    void setUp() {
        // User 모델의 생성자 파라미터 순서 및 타입에 맞게 수정 (email 필드가 있다면 추가)
        // 예시: testUser = new User("testId", "testUsername", encodedPassword, "test@example.com", "ROLE_USER");
        // 현재 User 모델에는 email과 role을 받는 생성자가 있습니다.
        // AuthServiceImpl의 register는 role을 내부적으로 결정하므로, 테스트 데이터 설정 시 참고합니다.
        testUser = new User("testId", "testUsername", encodedPassword, "test@example.com", "ROLE_USER");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(rawPassword, testUser.getPassword())).willReturn(true);

        // when
        boolean result = authService.login(testUser.getId(), rawPassword); // 이제 AuthServiceImpl의 실제 login 메소드가 호출됩니다.

        // then
        assertTrue(result);
        verify(userRepository).findById(testUser.getId());
        verify(passwordEncoder).matches(rawPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_failure_notFound() {
        // given
        given(userRepository.findById("unknownuser")).willReturn(Optional.empty());

        // when & then
        assertThrows(UsernameNotFoundException.class, () -> {
            authService.login("unknownuser", "password");
        }); // 메시지 검증은 선택사항

        // then
        verify(userRepository).findById("unknownuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_failure_passwordMismatch() {
        // given
        given(userRepository.findById(testUser.getId())).willReturn(Optional.of(testUser));
        given(passwordEncoder.matches(rawPassword, testUser.getPassword())).willReturn(false);

        // when & then
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(testUser.getId(), rawPassword);
        });

        // then
        verify(userRepository).findById(testUser.getId());
        verify(passwordEncoder).matches(rawPassword, testUser.getPassword());
    }

    @Test
    @DisplayName("회원가입 성공 - 일반 사용자")
    void register_success_roleUser() {
        String newUserId = "newuser";
        String newUserEmail = "newuser@example.com";
        String newRawPassword = "newpassword";
        String newEncodedPassword = "encodedNewPassword";

        given(userRepository.existsById(newUserId)).willReturn(false);
        given(userRepository.existsByEmail(newUserEmail)).willReturn(false);
        given(passwordEncoder.encode(newRawPassword)).willReturn(newEncodedPassword);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean result = authService.register(newUserId, "New User Name", newRawPassword, newUserEmail);

        // then
        assertTrue(result);
        verify(userRepository).existsById(newUserId);
        verify(userRepository).existsByEmail(newUserEmail);
        verify(passwordEncoder).encode(newRawPassword);
        verify(userRepository).save(argThat(user ->
                user.getId().equals(newUserId) &&
                        user.getUsername().equals("New User Name") &&
                        user.getPassword().equals(newEncodedPassword) &&
                        "ROLE_USER".equals(user.getRole()) // 문자열 비교 시 equals 사용 권장
        ));
    }

    @Test
    @DisplayName("회원가입 성공 - 관리자 (admin)")
    void register_success_roleAdmin_forAdminId() {
        // given
        String adminId = "admin";
        String adminEmail = "admin@example.com";
        String adminRawPassword = "adminpassword";
        String adminEncodedPassword = "encodedAdminPassword";

        given(userRepository.existsById(adminId)).willReturn(false);
        given(userRepository.existsByEmail(adminEmail)).willReturn(false); // 이메일 중복 검사 Mock 추가
        given(passwordEncoder.encode(adminRawPassword)).willReturn(adminEncodedPassword);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));


        // when
        boolean result = authService.register(adminId, "Admin User", adminRawPassword, adminEmail); // email 인자 전달

        // then
        assertTrue(result);
        verify(userRepository).existsById(adminId);
        verify(userRepository).existsByEmail(adminEmail);
        verify(passwordEncoder).encode(adminRawPassword);
        verify(userRepository).save(argThat(user ->
                user.getId().equals(adminId) &&
                        "ROLE_ADMIN".equals(user.getRole())
        ));
    }


    @Test
    @DisplayName("회원가입 성공 - 관리자 (root)")
    void register_success_roleAdmin_forRootId() {
        // given
        String rootId = "root";
        String rootEmail = "root@example.com"; // email 파라미터 추가
        String rootRawPassword = "rootpassword";
        String rootEncodedPassword = "encodedRootPassword";

        given(userRepository.existsById(rootId)).willReturn(false);
        given(userRepository.existsByEmail(rootEmail)).willReturn(false); // 이메일 중복 검사 Mock 추가
        given(passwordEncoder.encode(rootRawPassword)).willReturn(rootEncodedPassword);
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        boolean result = authService.register(rootId, "Root User", rootRawPassword, rootEmail); // email 인자 전달

        // then
        assertTrue(result);
        verify(userRepository).existsById(rootId); // existsById 호출 검증 추가
        verify(userRepository).existsByEmail(rootEmail); // existsByEmail 호출 검증 추가
        verify(passwordEncoder).encode(rootRawPassword); // encode 호출 검증 추가
        verify(userRepository).save(argThat(user ->
                user.getId().equals(rootId) &&
                        "ROLE_ROOT".equals(user.getRole())
        ));
    }

    // 문제가 발생했던 테스트
    @Test
    @DisplayName("회원가입 실패 테스트 - 이미 ID 존재")
    void register_failure_idExists() {
        // given
        given(userRepository.existsById("existinguser")).willReturn(true);
        // ID가 이미 존재하면, AuthServiceImpl의 로직상 existsByEmail은 호출되지 않습니다.

        // when
        boolean result = authService.register("existinguser", "Some User", "password", "some@example.com");

        // then
        assertFalse(result);
        verify(userRepository).existsById("existinguser"); // 이 메소드가 호출되어야 합니다.

        // ID가 이미 존재하면 || 연산자의 short-circuit에 의해 existsByEmail은 호출되지 않습니다.
        verify(userRepository, never()).existsByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 이메일 존재")
    void register_failure_emailExists() {
        // given
        String newUserId = "newuser2";
        String existingEmail = "existing@example.com";

        given(userRepository.existsById(newUserId)).willReturn(false);
        given(userRepository.existsByEmail(existingEmail)).willReturn(true);

        // when
        boolean result = authService.register(newUserId, "Another User", "password123", existingEmail);

        // then
        assertFalse(result);
        verify(userRepository).existsById(newUserId);
        verify(userRepository).existsByEmail(existingEmail);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}