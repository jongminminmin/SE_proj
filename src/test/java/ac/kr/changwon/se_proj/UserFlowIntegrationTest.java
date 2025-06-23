package ac.kr.changwon.se_proj;

import ac.kr.changwon.se_proj.dto.LoginRequestDTO;
import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.Project;
import ac.kr.changwon.se_proj.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // 실제 스프링 부트 애플리케이션 컨텍스트를 로드합니다.
@AutoConfigureMockMvc // MockMvc를 사용하여 실제 HTTP 요청을 시뮬레이션합니다.
@Transactional // 각 테스트 후 DB 변경 사항을 롤백하여 테스트 간 독립성을 보장합니다.
@ActiveProfiles("test") // application-test.properties를 활성화하여 H2 DB를 사용합니다.
@DisplayName("사용자 시나리오 통합 테스트")
public class UserFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository; // DB 상태를 직접 검증하기 위해 실제 리포지토리를 주입받습니다.

    @Test
    @DisplayName("시나리오: 회원가입 -> 로그인 -> 프로젝트 생성")
    void userRegistrationAndProjectCreationScenario() throws Exception {

        // =================================================================
        // 절차 1: 회원가입
        // =================================================================
        UserDto registerDto = UserDto.builder()
                .id("integrationUser")
                .username("통합테스트유저")
                .password("test!12345") // 유효성 검사 통과
                .email("integration@test.com")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(print());

        // =================================================================
        // 절차 2: 로그인
        // =================================================================
        LoginRequestDTO loginDto = new LoginRequestDTO();
        loginDto.setUserId("integrationUser");
        loginDto.setPassword("test!12345");

        // perform() 요청을 실행하고 그 결과를 MvcResult에 저장합니다.
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(print())
                .andReturn(); // 결과를 반환받아 세션 쿠키를 추출합니다.

        // 로그인 성공 후 생성된 세션 쿠키(JSESSIONID)를 추출합니다.
        Cookie sessionCookie = loginResult.getResponse().getCookie("JSESSIONID");
        assertThat(sessionCookie).isNotNull(); // 세션 쿠키가 생성되었는지 확인

        // =================================================================
        // 절차 3: 프로젝트 생성 (인증 필요)
        // =================================================================
        ProjectRequestDTO projectRequest = new ProjectRequestDTO();
        projectRequest.setProjectTitle("로그인 후 첫 프로젝트");
        projectRequest.setDescription("통합 테스트로 생성된 프로젝트입니다.");
        projectRequest.setOwnerId("integrationUser");
        projectRequest.setDate(LocalDate.now());
        projectRequest.setProjectMemberTier("BASIC");

        mockMvc.perform(post("/api/projects")
                        .cookie(sessionCookie) // <-- 이전 단계에서 얻은 세션 쿠키를 요청에 포함하여 인증 상태를 유지합니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectRequest)))
                .andExpect(status().isOk())
                .andDo(print());

        // =================================================================
        // 절차 4: DB 상태 직접 검증
        // 기대 결과: 프로젝트가 DB에 저장되고, 소유자가 정확히 연결되었는지 확인
        // =================================================================
        List<Project> projects = projectRepository.findAll();
        assertThat(projects).hasSize(1); // DB에 프로젝트가 1개만 있는지 확인

        Project createdProject = projects.get(0);
        assertThat(createdProject.getProjectTitle()).isEqualTo("로그인 후 첫 프로젝트");
        assertThat(createdProject.getOwner().getId()).isEqualTo("integrationUser");

        System.out.println("통합 테스트 성공: 회원가입부터 프로젝트 생성까지의 흐름이 정상적으로 동작함을 확인했습니다.");
    }
}
