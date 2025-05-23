package ac.kr.changwon.se_proj.controller.webSocket;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.properties.ChatProperties;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);


    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatProperties chatProps;


    /**
     * WebSocket 메시지 수신 처리
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO message, Principal principal) {
        log.info("Received message in sendMessage: {}, Principal: {}",
                (message != null ? "Content: " + message.getContent() + ", RoomId: " + message.getRoomId() + ", DTO_User: " + message.getUsername() : "null_message"),
                (principal != null ? principal.getName() : "null_principal"));

        int privateRoomMaxId = chatProps.getPrivateMaxRoomId();
        log.debug("privateRoomMaxId from chatProps: {}", privateRoomMaxId);

        String usernameFromClient = (principal != null) ? principal.getName() : message.getUsername();
        log.info("Username to find in repository: {}", usernameFromClient);

        // try-catch 블록 제거하고 orElseThrow가 직접 예외를 던지도록 함
        User sender = userRepository.findByUsername(usernameFromClient)
                .orElseThrow(() -> {
                    // 이 로그는 UsernameNotFoundException이 발생하기 직전에 찍힘
                    log.error("User not found for username: {}. Throwing UsernameNotFoundException.", usernameFromClient);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + usernameFromClient);
                });
        log.info("Sender found: {}", sender.getUsername()); // 예외 발생 시 이 라인은 실행

        //DB 저장
        ChatMessage chat = ChatMessage.builder()
                .sender(sender)
                .content(Objects.requireNonNull(message).getContent())
                .username(sender.getUsername())
                .timestamp(message.getTimestamp())
                .roomId(message.getRoomId())
                .receiverId(message.getReceiverId())
                .build();
        try {
            chatMessageRepository.save(chat);
            log.info("Chat message saved to DB: RoomId={}, Sender={}", chat.getRoomId(), chat.getSender().getUsername());
        }
        catch (Exception e) {
            log.error("Error saving chat message to DB, but will attempt to send. Error: {}", e.getMessage(), e);
            // DB 저장 실패 시에도 메시지는 전송 시도할지, 아니면 여기서 중단할지 정책 필요
        }

        //분기 설정. roomId필드 기준으로 Private, Group 구분
        String destination;
        if(message.getRoomId() <= privateRoomMaxId){
            destination = "/topic/private/" + message.getRoomId();
        }
        else{
            destination = "/topic/group/" + message.getRoomId();
        }
        log.info("Calculated destination: {}", destination);

        // 클라이언트로 보낼 DTO 업데이트
        message.setSenderId(sender.getId().toString()); // User ID가 Long이라면 String으로 변환, String이면 그대로
        message.setUsername(sender.getUsername()); // 실제 발신자 username으로 설정 (일관성)
        message.setTimestamp(chat.getTimestamp()); // DB에 저장된 시간 (또는 DTO의 시간)으로 통일

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.info("Message sent to destination: {}, Payload Content: {}", destination, message.getContent());
        } catch (Exception e) {
            log.error("Error sending message via messagingTemplate to {}. Error: {}", destination, e.getMessage(), e);
        }

    }



}
