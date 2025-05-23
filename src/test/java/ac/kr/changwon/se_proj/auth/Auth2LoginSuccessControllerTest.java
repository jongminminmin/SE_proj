package ac.kr.changwon.se_proj.auth; // 실제 패키지 경로 확인

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// static import 추가
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class Auth2LoginSuccessControllerTest { // 로그에 나온 클래스 이름 사용

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OAuth2 로그인 성공 시 루트(/)로 리디렉션 테스트")
    void oauth2Success_shouldRedirectToRoot() throws Exception {
        // 1. Mock OAuth2User 생성
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", "Test OAuth2 User"); // 실제 사용하는 사용자 속성 키로 대체 (예: "email", "sub" 등)
        attributes.put("email", "testuser@example.com");
        OAuth2UserAuthority authority = new OAuth2UserAuthority("ROLE_USER", attributes);
        OAuth2User mockOAuth2User = new DefaultOAuth2User(
                Collections.singleton(authority),
                attributes,
                "name" // "nameAttributeKey" - 사용자 이름(principal name)을 가져올 때 사용할 속성 키
        );

        // 2. Mock OAuth2AuthenticationToken 생성
        // "clientRegistrationId"는 실제 OAuth2 클라이언트 등록 ID (예: "google", "kakao")와 일치시킬 필요는 없으며, 테스트용 임의의 문자열도 가능합니다.
        OAuth2AuthenticationToken mockAuthentication = new OAuth2AuthenticationToken(
                mockOAuth2User,
                Collections.emptyList(), // authorities - 여기서는 mockOAuth2User 내부의 것을 사용하므로 비워둘 수 있음
                "test-client-registration-id" // authorizedClientRegistrationId
        );

        // 3. MockMvc 요청 시 .with(authentication(mockAuthentication)) 추가
        mockMvc.perform(get("/oauth2/success")
                        .with(authentication(mockAuthentication))) // 인증된 OAuth2 사용자 시뮬레이션
                .andExpect(status().is3xxRedirection()) // 302 리다이렉션 상태 확인
                .andExpect(redirectedUrl("/")); // 루트("/")로 리디렉션되는지 확인
    }
}