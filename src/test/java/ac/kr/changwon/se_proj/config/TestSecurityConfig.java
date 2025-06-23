package ac.kr.changwon.se_proj.config; // src/test/java 아래의 config 패키지에 위치

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration // 테스트 전용 설정 클래스임을 명시
public class TestSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 실제 설정과 동일하게 CSRF 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        // 실제 설정의 접근 허용 규칙을 그대로 가져와서 테스트 환경을 일치시킴
                        .requestMatchers(
                                "/ws/**", "/wss/**", "/api/chat/**",
                                "/", "/index.html", "/favicon.ico", "/manifest.json",
                                "/robots.txt", "/asset-manifest.json", "/static/**",
                                "/*.png", "/*.js", "/*.css",
                                "/api/auth/**", "/oauth2/**",
                                "/login", "/register", "/main", "/chat/**"
                        ).permitAll()
                        // 그 외 모든 요청은 인증을 요구하도록 설정
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
