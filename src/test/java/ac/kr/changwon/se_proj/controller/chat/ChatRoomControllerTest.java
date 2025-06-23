package ac.kr.changwon.se_proj.controller.chat;

import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.dto.ChatRoomCreationRequest;
import ac.kr.changwon.se_proj.dto.ChatRoomDTO;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.impl.ChatMessageServiceImpl;
import ac.kr.changwon.se_proj.service.impl.ChatRoomServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@Import(TestSecurityConfig.class)
@DisplayName("ChatRoomController 단위 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatRoomRepository chatRoomRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ChatRoomServiceImpl chatRoomService;

    @MockBean
    private ChatMessageServiceImpl chatMessageService;

    @MockBean
    private SimpMessageSendingOperations messagingTemplate; // Controller가 의존하므로 MockBean으로 추가

    @MockBean
    private UserDetailsService userDetailsService;

    private User testUser;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 설정
        testUser = new User();
        testUser.setId("testUser");
        testUser.setUsername("테스트유저");

        // 테스트용 채팅방 설정
        chatRoom = ChatRoom.builder()
                .id(UUID.randomUUID().toString())
                .intId(1)
                .name("테스트 채팅방")
                .type("PRIVATE")
                .lastMessage("마지막 메시지")
                .lastMessageTime(LocalDateTime.now())
                .userChatRooms(new HashSet<>())
                .build();

        // 사용자와 채팅방 연결 설정
        UserChatRoom userChatRoom = UserChatRoom.builder()
                .user(testUser)
                .chatRoom(chatRoom)
                .unreadCount(5)
                .build();
        chatRoom.getUserChatRooms().add(userChatRoom);
    }

    @Test
    @WithMockUser(username = "testUser") // "testUser" 라는 아이디로 로그인한 상태를 시뮬레이션
    @DisplayName("내 채팅방 목록 조회 테스트")
    void getMyChatRooms_success() throws Exception {
        // given
        // userRepository가 "testUser"를 찾으면 위에서 만든 testUser 객체를 반환하도록 설정
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(testUser));
        // chatRoomService가 "testUser"의 ID로 채팅방 목록을 요청하면 위에서 만든 chatRoom 객체 리스트를 반환하도록 설정
        given(chatRoomService.getChatRoomsByUserId(testUser.getId())).willReturn(Collections.singletonList(chatRoom));

        // when
        ResultActions resultActions = mockMvc.perform(get("/api/chats/my-rooms"));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chatRoom.getId()))
                .andExpect(jsonPath("$[0].name").value("테스트 채팅방"))
                .andExpect(jsonPath("$[0].unreadCount").value(5)) // 읽지 않은 메시지 수 검증
                .andDo(print());
    }

    @Test
    @WithMockUser(username = "creatorUser") // "creatorUser"로 로그인
    @DisplayName("1:1 개인 채팅방 생성 테스트")
    void createPrivateChatRoom_success() throws Exception {
        // given
        User creatorUser = new User();
        creatorUser.setId("creatorUser");
        User otherUser = new User();
        otherUser.setId("otherUser");

        // 요청 DTO 생성
        ChatRoomCreationRequest request = new ChatRoomCreationRequest();
        request.setType("PRIVATE");
        request.setUser2Id("otherUser");

        // 생성될 채팅방 객체 준비
        ChatRoom newPrivateRoom = ChatRoom.builder()
                .id(UUID.randomUUID().toString())
                .intId(2)
                .name("creatorUser,otherUser")
                .type("PRIVATE")
                .userChatRooms(new HashSet<>())
                .build();
        newPrivateRoom.getUserChatRooms().add(new UserChatRoom(creatorUser, newPrivateRoom));
        newPrivateRoom.getUserChatRooms().add(new UserChatRoom(otherUser, newPrivateRoom));

        // Mock 설정
        given(userRepository.findById("creatorUser")).willReturn(Optional.of(creatorUser));
        given(userRepository.findById("otherUser")).willReturn(Optional.of(otherUser));
        given(chatRoomService.createOrGetPrivateChatRoom(any(User.class), any(User.class))).willReturn(newPrivateRoom);

        // when
        ResultActions resultActions = mockMvc.perform(post("/api/chats/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // then
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newPrivateRoom.getId()))
                .andExpect(jsonPath("$.type").value("PRIVATE"))
                .andExpect(jsonPath("$.participants").value(2)) // 참여자 수 검증
                .andDo(print());
    }
}
