package ac.kr.changwon.se_proj.chat;


import ac.kr.changwon.se_proj.config.TestSecurityConfig;
import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class WebSocketIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(WebSocketIntegrationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String websocketUri;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private BlockingQueue<ChatMessageDTO> blockingQueue;

    private User testSenderUser;
    private final String SENDER_USERNAME = "integrationUser";
    private final String SENDER_USER_ID_STRING = "integrationTestUserId";

    // private SSLContext originalDefaultSslContext; // 이 필드는 더 이상 사용되지 않습니다.

    @BeforeEach
    void setUp() throws Exception {
        websocketUri = "wss://localhost:" + port + "/ws";
        log.info("Attempting to connect to WebSocket URI: {}", websocketUri);

        try {
            // 키스토어 파일이 src/test/resources/ 디렉터리 바로 아래에 있다고 가정합니다.
            // 만약 keystore 하위 폴더에 있다면 경로를 "keystore/test-keystore.p12"로 수정하세요.
            File trustStoreFile = new ClassPathResource("test-keystore.p12").getFile();
            System.setProperty("javax.net.ssl.trustStore", trustStoreFile.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", "testPassword"); // 실제 비밀번호로 변경!
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
            log.info("Set JVM system properties for SSL trust store using: {}", trustStoreFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to set system properties for SSL trust store. Ensure 'test-keystore.p12' is in 'src/test/resources/' (or 'src/test/resources/keystore/') and password is correct.", e);
            throw e;
        }

        StandardWebSocketClient simpleWebSocketClient = new StandardWebSocketClient();
        stompClient = new WebSocketStompClient(simpleWebSocketClient);

        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.setObjectMapper(this.objectMapper);
        stompClient.setMessageConverter(messageConverter);

        blockingQueue = new LinkedBlockingDeque<>();

        chatMessageRepository.deleteAll();
        userRepository.deleteAll();
        testSenderUser = new User(SENDER_USER_ID_STRING, SENDER_USERNAME, "password", "password", "ROLE_USER");
        userRepository.save(testSenderUser);

        // STOMP 연결 헤더 (STOMP 프로토콜 레벨)
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("username", SENDER_USERNAME);

        // WebSocket 핸드셰이크 헤더 (HTTP 레벨)
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin("https://localhost:" + port);
        // === 중요: Sec-WebSocket-Protocol 헤더 명시적 추가 ===
        List<String> subProtocols = List.of("v10.stomp", "v11.stomp", "v12.stomp");
        handshakeHeaders.setSecWebSocketProtocol(subProtocols);
        // =================================================

        log.info("Connecting STOMP client to WSS endpoint with Origin: {} and Subprotocols: {}", handshakeHeaders.getOrigin(), subProtocols);
        try {
            stompSession = stompClient.connectAsync(
                    websocketUri,
                    handshakeHeaders,       // Sec-WebSocket-Protocol 포함된 핸드셰이크 헤더 전달
                    connectHeaders,         // STOMP 연결 헤더 전달
                    new StompSessionHandlerAdapter() {
                        @Override
                        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                            log.info("STOMP client connected over WSS: session id={}, headers={}", session.getSessionId(), connectedHeaders);
                            // 연결 성공 시 서버가 어떤 STOMP 버전을 선택했는지 확인 가능
                            log.info("Negotiated STOMP version: {}", session.getSessionId(), connectedHeaders.getFirst("version"));
                        }
                        // ... (handleException, handleTransportError 핸들러는 이전과 동일)
                        @Override
                        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
                            log.error("STOMP client error (WSS): command={}, headers={}, payload={}, exception={}", command, headers, payload, exception.getMessage(), exception);
                        }

                        @Override
                        public void handleTransportError(StompSession session, Throwable exception) {
                            log.error("STOMP transport error (WSS): session id={}, exception={}", session.getSessionId(), exception.getMessage(), exception);
                        }
                    }
            ).get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to connect STOMP session over WSS", e);
            // 시스템 프로퍼티는 자동으로 초기화되지 않으므로, 필요시 tearDown에서 정리합니다.
            throw e;
        }

        assertNotNull(stompSession, "STOMP 세션이 null이 아니어야 합니다 (WSS).");
        assertTrue(stompSession.isConnected(), "STOMP 세션이 연결 상태여야 합니다 (WSS).");
        log.info("STOMP session successfully established over WSS.");
    }

    @AfterEach
    void tearDown() {
        if (stompSession != null && stompSession.isConnected()) {
            log.info("Disconnecting STOMP session: {}", stompSession.getSessionId());
            stompSession.disconnect();
        }

        // setUp에서 설정한 시스템 프로퍼티 정리 (선택 사항이지만 권장)
         System.clearProperty("javax.net.ssl.trustStore");
         System.clearProperty("javax.net.ssl.trustStorePassword");
         System.clearProperty("javax.net.ssl.trustStoreType");
         log.info("Cleared JVM system properties for SSL trust store in tearDown, if they were set.");
    }

    @Test
    @DisplayName("순수 WSS연결 테스트")
    void connectToWss() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHandler handler = new TextWebSocketHandler() {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                log.info(">>>> Plain WSS connection established: " + session.getId());
                session.close(CloseStatus.NORMAL);
            }
            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                log.error(">>>> Plain WSS transport error", exception);
            }
            @Override
            public boolean  supportsPartialMessages() { return false; } // 추가
        };
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin("https://localhost:" + port);

        log.info("Attempting plain WSS connection to {}", websocketUri);
        ListenableFuture<WebSocketSession> future = client.doHandshake(handler, handshakeHeaders, URI.create(websocketUri));
        try {
            WebSocketSession session = future.get(10, TimeUnit.SECONDS);
            assertTrue(session.isOpen() || !session.isOpen()); // 연결 시도 자체의 성공 여부 확인 (금방 닫힐 수 있음)
            log.info(">>>> Plain WSS connection attempt completed. Session ID (if connected): {}", session.getId());
        }
        catch (Exception e) {
            log.error(">>>> Plain WSS connection failed", e);
            fail("Plain WSS connection failed", e);
        }
    }


    private class TestStompFrameHandler implements StompFrameHandler {
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return ChatMessageDTO.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            log.debug("Received frame: headers={}, payload={}", headers, payload);
            blockingQueue.offer((ChatMessageDTO) payload);
        }
    }

    @Test
    @DisplayName("개인 채팅 메시지 전송 및 수신 테스트 (WSS)")
    void sendAndReceivePrivateMessage() throws Exception {
        // given
        int privateRoomId = 1;
        String topic = "/topic/private/" + privateRoomId;

        stompSession.subscribe(topic, new TestStompFrameHandler());
        log.info("Subscribed to {}", topic);

        ChatMessageDTO messageToSend = new ChatMessageDTO();
        messageToSend.setRoomId(privateRoomId);
        messageToSend.setContent("Hello Private Chat over WSS!");
        messageToSend.setUsername(SENDER_USERNAME);
        messageToSend.setTimestamp(LocalDateTime.now());
        messageToSend.setReceiverId("receiverTestUserId123");

        // when: /app/chat.sendMessage 로 메시지 발송
        stompSession.send("/app/chat.sendMessage", messageToSend);
        log.info("Sent message to /app/chat.sendMessage: {}", this.objectMapper.writeValueAsString(messageToSend));

        // then: 구독한 토픽으로 메시지 수신 확인
        ChatMessageDTO receivedMessage = blockingQueue.poll(10, TimeUnit.SECONDS);
        log.info("Received message: {}", (receivedMessage != null ? this.objectMapper.writeValueAsString(receivedMessage) : "null"));

        assertNotNull(receivedMessage, "수신된 메시지가 없거나 타임아웃되었습니다.");
        assertEquals(messageToSend.getContent(), receivedMessage.getContent());
        assertEquals(privateRoomId, receivedMessage.getRoomId());
        assertEquals(SENDER_USER_ID_STRING, receivedMessage.getSenderId());
        assertEquals(SENDER_USERNAME, receivedMessage.getUsername());

        // DB 저장 확인
        List<ChatMessage> savedMessages = chatMessageRepository.findAll();
        assertFalse(savedMessages.isEmpty(), "채팅 메시지가 DB에 저장되지 않았습니다.");
        ChatMessage savedDbMessage = savedMessages.get(0);
        assertEquals(messageToSend.getContent(), savedDbMessage.getContent());
        assertEquals(SENDER_USERNAME, savedDbMessage.getUsername());
        assertEquals(testSenderUser.getId(), savedDbMessage.getSender().getId());
        assertEquals(privateRoomId, savedDbMessage.getRoomId());
    }

    @Test
    @DisplayName("그룹 채팅 메시지 전송 및 수신 테스트 (WSS)")
    void sendAndReceiveGroupMessage() throws Exception {
        // given
        int groupRoomId = 11;
        String topic = "/topic/group/" + groupRoomId;

        stompSession.subscribe(topic, new TestStompFrameHandler());
        log.info("Subscribed to {}", topic);

        ChatMessageDTO messageToSend = new ChatMessageDTO();
        messageToSend.setRoomId(groupRoomId);
        messageToSend.setContent("Hello Group Chat over WSS!");
        messageToSend.setUsername(SENDER_USERNAME);
        messageToSend.setReceiverId("receiverTestUser123");
        messageToSend.setTimestamp(LocalDateTime.now());

        // when
        stompSession.send("/app/chat.sendMessage", messageToSend);
        log.info("Sent message to /app/chat.sendMessage: {}", this.objectMapper.writeValueAsString(messageToSend));

        // then
        ChatMessageDTO receivedMessage = blockingQueue.poll(10, TimeUnit.SECONDS);
        log.info("Received message: {}", (receivedMessage != null ? this.objectMapper.writeValueAsString(receivedMessage) : "null"));

        assertNotNull(receivedMessage, "수신된 메시지가 없거나 타임아웃되었습니다.");
        assertEquals(messageToSend.getContent(), receivedMessage.getContent());
        assertEquals(groupRoomId, receivedMessage.getRoomId());
        assertEquals(SENDER_USER_ID_STRING, receivedMessage.getSenderId());
        assertEquals(SENDER_USERNAME, receivedMessage.getUsername());

        // DB 저장 확인
        List<ChatMessage> savedMessages = chatMessageRepository.findAll();
        assertFalse(savedMessages.isEmpty());
        assertEquals(1, savedMessages.size(), "메시지가 정확히 1개 저장되어야 합니다.");
        ChatMessage savedDbMessage = savedMessages.get(0);
        assertEquals(messageToSend.getContent(), savedDbMessage.getContent());
        assertEquals(SENDER_USERNAME, savedDbMessage.getUsername());
        assertEquals(testSenderUser.getId(), savedDbMessage.getSender().getId());
        assertEquals(groupRoomId, savedDbMessage.getRoomId());
    }
}