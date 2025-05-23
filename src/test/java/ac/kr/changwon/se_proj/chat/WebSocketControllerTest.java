package ac.kr.changwon.se_proj.chat;


import ac.kr.changwon.se_proj.controller.webSocket.WebSocketController;
import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.properties.ChatProperties;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private ChatProperties chatProperties;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WebSocketController webSocketController;

    private ChatMessageDTO chatMessageDTO;
    private User testUser;
    private Principal mockPrincipal;

    @BeforeEach
    void setUp(){
        chatMessageDTO = new ChatMessageDTO();
        chatMessageDTO.setRoomId(1);//개인 채팅방 기본 설정
        chatMessageDTO.setContent("testContent");
        chatMessageDTO.setUsername("testUser"); // DTO의 기본 username 설정
        chatMessageDTO.setTimestamp(LocalDateTime.now());

        testUser = new User("testId", "testUsername", "encodedPassword", "<PASSWORD>", "ROLE_USER");

        mockPrincipal = mock(Principal.class);

        when(chatProperties.getPrivateMaxRoomId()).thenReturn(10);
    }


    @Test
    @DisplayName("sendMessage - if exist Principal then sucess sending Message")
    void sendMessage_withPrincipal_sendsPrivateMessageSuccessfully(){
        // Principal을 사용하는 테스트이므로, mockPrincipal.getName()에 대한 stubbing을 여기에 추가합니다.
        when(mockPrincipal.getName()).thenReturn("testUsername");

        // chatMessageDTO의 roomId를 개인 채팅방으로 설정 (setUp에서 이미 1로 되어 있지만 명시적으로)
        chatMessageDTO.setRoomId(1);

        // userRepository.findByUsername 호출 시 mockPrincipal.getName()의 반환값("testUsername")을 사용합니다.
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(testUser));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        webSocketController.sendMessage(chatMessageDTO, mockPrincipal);

        // then
        verify(userRepository).findByUsername("testUsername");
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/private/1"), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("sendMessage - Principal null, DTO username 사용 시 그룹 메시지 전송 성공")
    void sendMessage_withoutPrincipal_sendsGroupMessageSuccessfully() {
        chatMessageDTO.setRoomId(11); // 그룹 채팅방 ID로 변경
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        webSocketController.sendMessage(chatMessageDTO, null); // Principal null 전달

        // then
        verify(userRepository).findByUsername("testUser");
        verify(chatMessageRepository).save(any(ChatMessage.class));
        // chatProperties.getPrivateMaxRoomId()가 10으로 설정되어 있고, roomId가 11이므로 그룹 목적지로 가야 함
        verify(messagingTemplate).convertAndSend(eq("/topic/group/11"), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("sendMessage - 사용자 찾을 수 없을 때 UsernameNotFoundException 발생")
    void sendMessage_userNotFound_throwsUsernameNotFoundException() {
        // Principal을 사용하는 테스트이므로, mockPrincipal.getName()에 대한 stubbing을 여기에 추가합니다.
        when(mockPrincipal.getName()).thenReturn("testUsername");

        // given
        // userRepository.findByUsername 호출 시 mockPrincipal.getName()의 반환값("testUsername")을 사용합니다.
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.empty());

        // when & then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            webSocketController.sendMessage(chatMessageDTO, mockPrincipal);
        });
        assertEquals("사용자를 찾을 수 없습니다: testUsername", exception.getMessage());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("sendMessage - 메시지 저장 및 전송 시 DTO 필드 업데이트 확인")
    void sendMessage_updatesDtoFieldsCorrectly() {
        // Principal을 사용하는 테스트이므로, mockPrincipal.getName()에 대한 stubbing을 여기에 추가합니다.
        when(mockPrincipal.getName()).thenReturn("testUsername");

        // chatMessageDTO의 roomId를 개인 채팅방으로 설정 (setUp에서 이미 1로 되어 있지만 명시적으로)
        chatMessageDTO.setRoomId(1);

        // given
        // userRepository.findByUsername 호출 시 mockPrincipal.getName()의 반환값("testUsername")을 사용합니다.
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(testUser));

        ChatMessage savedMessage = ChatMessage.builder()
                .sender(testUser) // testUser의 username은 "testUsername"
                .content(chatMessageDTO.getContent())
                .username(testUser.getUsername()) // "testUsername"
                .timestamp(chatMessageDTO.getTimestamp())
                .roomId(chatMessageDTO.getRoomId())
                .build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);



        webSocketController.sendMessage(chatMessageDTO, mockPrincipal);

        assertEquals(String.valueOf(testUser.getId()), chatMessageDTO.getSenderId());
        assertEquals(savedMessage.getTimestamp(), chatMessageDTO.getTimestamp());
        verify(messagingTemplate).convertAndSend(eq("/topic/private/1"), eq(chatMessageDTO));
    }
}