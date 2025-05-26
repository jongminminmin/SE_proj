package ac.kr.changwon.se_proj.config;

import ac.kr.changwon.se_proj.service.Interface.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;


/* 스프링 자체에서 제공하는 보안 관련 컨픽 클래스*/
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityContext(context -> context
                        .securityContextRepository(new HttpSessionSecurityContextRepository()))
                // .requiresChannel(channel -> channel.anyRequest().requiresSecure()) // HTTPS 강제는 필요시 프로덕션 환경에서 웹서버/로드밸런서 레벨에서 처리 권장
                .authorizeHttpRequests(auth -> auth
                        // React App 정적 리소스 및 기본 경로 허용
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/manifest.json",
                                "/robots.txt",
                                "/asset-manifest.json", // create-react-app 빌드 시 생성될 수 있음
                                "/static/**",          // React 빌드 결과물의 JS, CSS, Media 등
                                "/*.png",              // 루트 경로의 PNG 파일 (예: 로고)
                                "/*.js",
                                "/*.css"
                        ).permitAll()
                        // 기존 API 및 특정 경로 허용 규칙 유지
                        .requestMatchers(
                                "/api/auth/**",      // 인증 관련 API (회원가입, API 로그인 등)
                                "/oauth2/**"         // OAuth2 관련 경로
                                // "/login", "/register", "/main" 등은 이제 React Router가 처리하므로,
                                // 이 경로들로의 GET 요청은 MvcConfig에 의해 index.html로 연결되어 permitAll 효과를 가짐.
                                // 만약 특정 API 경로를 추가로 permitAll 해야 한다면 여기에 명시.
                                // 예: "/api/public/**"
                        ).permitAll()
                        // 특정 페이지 경로 (이들은 React 라우트가 되므로, GET 요청은 index.html로 연결되어 permitAll 효과)
                        // 만약 이 경로들 하위에 특정 API가 있고, 그 API를 permitAll 해야 한다면 명시적으로 추가
                        .requestMatchers("/main", "/login", "/register", "/chat/**", "/ws/**","/wss/**","/api/chat/**").permitAll() // 기존 permitAll 유지
                        // 그 외 모든 API 요청은 인증 필요
                        .requestMatchers("/api/**").authenticated()
                        // 나머지 모든 요청은 인증 필요 (위에서 permitAll 되지 않은 경우)
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        // 로그인 페이지 경로. React 앱에서는 이 경로로 접근 시 index.html이 서빙되고
                        // React Router가 로그인 컴포넌트를 렌더링합니다.
                        .loginPage("/login")
                        // 실제 로그인 처리 URL (Spring Security가 이 POST 요청을 처리)
                        .loginProcessingUrl("/login") // 이 URL은 AuthController의 API 로그인과 구분되어야 함.
                        .usernameParameter("userId")
                        .passwordParameter("password")
                        // 로그인 성공 시 리다이렉트될 URL. React 앱에서는 이 경로로 접근 시 index.html이 서빙되고
                        // React Router가 메인 컴포넌트를 렌더링합니다.
                        .defaultSuccessUrl("/main", true)
                        .failureUrl("/login?error=true") // 로그인 실패 시 리다이렉트 (React 라우트)
                        .permitAll() // /login, /login?error=true 등의 경로에 대한 접근 허용
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // 기본값은 /logout
                        .logoutSuccessUrl("/login?logout") // 로그아웃 성공 시 리다이렉트 (React 라우트)
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login") // OAuth2 로그인 시도 시 리다이렉트될 로그인 페이지 (React 라우트)
                        .defaultSuccessUrl("/oauth2/success", true) // OAuth2 성공 후 리다이렉트 (React 라우트 또는 서버 처리)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                )
                .sessionManagement(s -> s
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true)
                )
                .csrf(AbstractHttpConfigurer::disable); // CSRF는 SPA 환경에서 다르게 처리될 수 있음 (예: stateless API의 경우 disable, 또는 토큰 방식 사용)
        return http.build();

    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
