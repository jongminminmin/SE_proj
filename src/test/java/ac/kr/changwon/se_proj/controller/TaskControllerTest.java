package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.dto.TaskDTO;
import ac.kr.changwon.se_proj.dto.TaskRequestDTO;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.service.Interface.TaskService;
import ac.kr.changwon.se_proj.service.SseService;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(TestSecurityConfig.class)
@DisplayName("TaskController 단위 테스트")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private SseService sseService; // TaskController가 의존하므로 MockBean으로 추가

    @MockBean
    private UserDetailsService userDetailsService;

    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        // 여러 테스트에서 공통으로 사용할 TaskDTO 객체 설정
        UserDto userDto = UserDto.builder().id("testUser").username("테스트유저").email("test@test.com").build();
        taskDTO = new TaskDTO();
        taskDTO.setTaskNo(1);
        taskDTO.setProjectId(1L);
        taskDTO.setTaskTitle("테스트 업무");
        taskDTO.setDescription("테스트 업무 설명");
        taskDTO.setStatus("진행중");
        taskDTO.setAssignee(userDto);
    }

    @Test
    @WithMockUser // 인증된 사용자가 요청하는 것을 시뮬레이션
    @DisplayName("업무 생성 테스트")
    void createTask_success() throws Exception {
        // given
        TaskRequestDTO requestDTO = new TaskRequestDTO();
        requestDTO.setTaskTitle("새 업무");
        requestDTO.setProjectId(1L);

        // taskService.createTask가 호출되면, 준비된 taskDTO를 반환하도록 설정
        given(taskService.createTask(any(TaskRequestDTO.class))).willReturn(taskDTO);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value(1))
                .andExpect(jsonPath("$.taskTitle").value("테스트 업무"))
                .andDo(print());
    }

    @Test
    @WithMockUser
    @DisplayName("업무 상태 변경 테스트")
    void updateTaskStatus_success() throws Exception {
        // given
        Integer taskId = 1;
        String newStatus = "완료";
        Map<String, String> payload = new HashMap<>();
        payload.put("status", newStatus);

        // 상태가 변경된 DTO를 반환하도록 설정
        taskDTO.setStatus(newStatus);
        given(taskService.updateTaskStatus(taskId, newStatus)).willReturn(taskDTO);

        // when
        ResultActions resultActions = mockMvc.perform(put("/api/tasks/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskNo").value(taskId))
                .andExpect(jsonPath("$.status").value(newStatus))
                .andDo(print());
    }

    @Test
    @WithMockUser
    @DisplayName("업무 삭제 테스트")
    void deleteTask_success() throws Exception {
        // given
        Integer taskId = 1;
        // void를 반환하는 메소드는 doNothing()으로 설정
        doNothing().when(taskService).deleteTask(taskId);

        // when
        ResultActions resultActions = mockMvc.perform(delete("/api/tasks/{id}", taskId));

        // then
        resultActions
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @WithMockUser
    @DisplayName("특정 프로젝트의 업무 목록 조회 테스트")
    void getTasksByProjectId_success() throws Exception {
        // given
        Long projectId = 1L;
        List<TaskDTO> taskList = Collections.singletonList(taskDTO);
        given(taskService.findAllTasksByProjectId(anyLong())).willReturn(taskList);

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/tasks/project/{projectId}", projectId));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskNo").value(taskDTO.getTaskNo()))
                .andExpect(jsonPath("$[0].projectId").value(projectId))
                .andDo(print());
    }
}
