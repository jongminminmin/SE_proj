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
        chatMessageDTO.setRoomId(1);//개인 채팅방
        chatMessageDTO.setContent("testContent");
        chatMessageDTO.setUsername("testUser");
        chatMessageDTO.setTimestamp(LocalDateTime.now());

        testUser = new User("testId", "testUsername", "encodedPassword", "<PASSWORD>", "ROLE_USER");

        mockPrincipal = mock(Principal.class);

        when(mockPrincipal.getName()).thenReturn("testUsername");


        when(chatProperties.getPrivateMaxRoomId()).thenReturn(10);

    }


    @Test
    @DisplayName("sendMessage - if exist Principal then success sending Message")
    void sendMessage_withPrincipal_sendsPrivateMessageSuccessfully(){
        when(mockPrincipal.getName()).thenReturn("testUsername"); // Principal이 사용되므로 여기서 stubbing

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
        // given
        chatMessageDTO.setRoomId(11);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(testUser));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        webSocketController.sendMessage(chatMessageDTO, null);

        // then
        verify(userRepository).findByUsername("testUser");
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/group/11"), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("sendMessage - 사용자 찾을 수 없을 때 UsernameNotFoundException 발생")
    void sendMessage_userNotFound_throwsUsernameNotFoundException() {
        // given
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
        // given
        when(userRepository.findByUsername("testUsername")).thenReturn(Optional.of(testUser));
        // ChatMessage의 빌더는 그대로 사용 가능 (ChatMessage.java에 @Builder가 있으므로)
        ChatMessage savedMessage = ChatMessage.builder()
                .sender(testUser)
                .content(chatMessageDTO.getContent())
                .username(testUser.getUsername()) // User 객체의 getter 사용
                .timestamp(chatMessageDTO.getTimestamp())
                .roomId(chatMessageDTO.getRoomId())
                .build();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);


        // when
        webSocketController.sendMessage(chatMessageDTO, mockPrincipal);

        // then
        assertEquals(testUser.getId(), chatMessageDTO.getSenderId()); // User 객체의 getter 사용
        assertEquals(savedMessage.getTimestamp(), chatMessageDTO.getTimestamp());
        verify(messagingTemplate).convertAndSend(eq("/topic/private/1"), eq(chatMessageDTO));
    }


}



