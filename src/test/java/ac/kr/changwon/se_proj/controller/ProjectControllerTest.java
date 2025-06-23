package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.dto.ProjectDTO;
import ac.kr.changwon.se_proj.dto.ProjectRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.ProjectService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProjectController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ProjectController 단위 테스트")
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private UserDetailsService userDetailsService;

    private UserDto testUserDto;
    private ProjectDTO projectDTO;

    @BeforeEach
    void setUp() {
        // 테스트에 사용할 공통 DTO 객체 설정
        testUserDto = UserDto.builder()
                .id("testUser")
                .username("테스트유저")
                .email("test@test.com")
                .build();

        projectDTO = new ProjectDTO();
        projectDTO.setProjectId(1L);
        projectDTO.setProjectTitle("테스트 프로젝트");
        projectDTO.setDescription("이것은 테스트 프로젝트입니다.");
        projectDTO.setOwner(testUserDto);
        projectDTO.setMembers(new HashSet<>(Collections.singletonList(testUserDto)));
    }

    @Test
    @WithMockUser(username = "testUser") // username = "testUser"로 로그인한 상태 시뮬레이션
    @DisplayName("사용자 관련 프로젝트 목록 조회")
    void getAllProjectsForUser_success() throws Exception {
        // given
        // projectService.findAll()이 호출되면, 위에서 만든 projectDTO 리스트를 반환하도록 설정
        given(projectService.findAll(anyString())).willReturn(Collections.singletonList(projectDTO));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/projects"));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectId").value(1L))
                .andExpect(jsonPath("$[0].projectTitle").value("테스트 프로젝트"))
                .andExpect(jsonPath("$[0].owner.id").value("testUser"))
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "testUser")
    @DisplayName("프로젝트 생성")
    void createProject_success() throws Exception {
        // given
        ProjectRequestDTO requestDTO = new ProjectRequestDTO();
        requestDTO.setProjectTitle("새 프로젝트");
        requestDTO.setOwnerId("testUser");
        requestDTO.setDate(LocalDate.now());

        // void를 반환하는 메소드는 doNothing()으로 설정
        doNothing().when(projectService).createProject(any(ProjectRequestDTO.class));

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @WithMockUser
    @DisplayName("프로젝트 멤버 목록 조회")
    void getProjectMembers_success() throws Exception {
        // given
        Long projectId = 1L;
        // projectService.getMembersOfProject()가 호출되면 testUserDto 리스트를 반환하도록 설정
        given(projectService.getMembersOfProject(projectId)).willReturn(Collections.singletonList(testUserDto));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/projects/{id}/members", projectId));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("testUser"))
                .andExpect(jsonPath("$[0].username").value("테스트유저"))
                .andDo(print());
    }

    @Test
    @WithMockUser
    @DisplayName("프로젝트에 사용자 초대")
    void inviteUserToProject_success() throws Exception {
        // given
        Long projectId = 1L;
        String usernameToInvite = "newUser";
        Map<String, String> payload = new HashMap<>();
        payload.put("username", usernameToInvite);

        // void를 반환하는 메소드는 doNothing()으로 설정
        doNothing().when(projectService).addMemberToProject(projectId, usernameToInvite);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/projects/{projectId}/invite", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사용자를 프로젝트에 추가했습니다."))
                .andDo(print());
    }
}
