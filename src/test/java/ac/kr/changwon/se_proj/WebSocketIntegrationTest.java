package ac.kr.changwon.se_proj;

import ac.kr.changwon.se_proj.controller.chat.ChatMessageController;
import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket 통합 테스트: H2 인메모리 DB (test 프로파일) 사용
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class WebSocketIntegrationTest {

    private String senderUsername;
    private String receiverId;

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        List<Transport> transports = Collections.singletonList(
                new WebSocketTransport(new StandardWebSocketClient())
        );
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        //테스트용 사용자 생성
        User testUser = new User();
        testUser.setId("testSender");
        testUser.setUsername("testUser");
        testUser.setPassword("testPass");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        //수신자
        User testReceiver = new User();
        testReceiver.setId("testReceiver");
        testReceiver.setUsername("testReceiver");
        testReceiver.setPassword("<PASSWORD>");
        testReceiver.setRole("USER");
        testReceiver = userRepository.save(testReceiver);

        this.senderUsername = testUser.getUsername();
        this.receiverId = testReceiver.getId();
    }

    @Test
    public void testSendMessageAndReceive() throws Exception {
        String url = "ws://localhost:" + port + "/ws";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChatMessageDTO> receivedMessage = new AtomicReference<>();

        // STOMP 세션 연결 (비동기)
        StompSession session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(1, TimeUnit.SECONDS);

        // 수신자 구독 (receiverId=2)
        session.subscribe("/topic/private/2", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessageDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessage.set((ChatMessageDTO) payload);
                latch.countDown();
            }
        });

        // 메시지 전송
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setReceiverId(receiverId);
        dto.setRoomId(1);
        dto.setContent("Test message");
        dto.setUsername(senderUsername);
        session.send("/app/chat.sendMessage", dto);

        // 결과 검증
        assertTrue(latch.await(3, TimeUnit.SECONDS), "Message not received in time");
        assertNotNull(receivedMessage.get());
        assertEquals("Test message", receivedMessage.get().getContent());
        assertEquals(1L, receivedMessage.get().getRoomId());
    }
}
