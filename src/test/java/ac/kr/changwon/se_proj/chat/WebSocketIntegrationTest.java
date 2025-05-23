package ac.kr.changwon.se_proj.chat;


import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.BlockingQueue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class WebSocketIntegrationTest {

    @LocalServerPort
    private int port=33455;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;


    private WebSocketStompClient webSocketStompClient;
    private StompSession stompSession;
    private String webSocketUrl;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // 메시지를 비동기적으로 수신하기 위한 BlockingQueue
    private BlockingQueue<ChatMessageDTO> blockingQueue;

    // 테스트용 사용자 정보
    private User testSenderUser;
    private final String SENDER_USERNAME = "integrationUser";
    private final String SENDER_USER_ID_STRING = "integrationTestUserId"; // User 모델의 ID가 String이라고 가정



}
