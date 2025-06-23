package ac.kr.changwon.se_proj.controller.webSocket;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.dto.ChatReadStatusMessage;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.properties.ChatProperties;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository; // ChatRoomRepository 임포트 추가
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService;
import ac.kr.changwon.se_proj.service.impl.ChatMessageServiceImpl; // NEW
import ac.kr.changwon.se_proj.service.impl.ChatRoomServiceImpl; // 추가
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime; // LocalDateTime 임포트 추가
import java.util.Locale;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageServiceImpl chatMessageService;
    private final ChatRoomService chatRoomService;

    /**
     * WebSocket 메시지 수신 및 브로드캐스트 처리
     * @param message 클라이언트에서 보낸 메시지 DTO
     * @param roomIdString 목적지 변수 (채팅방 UUID)
     * @param principal 인증된 사용자 정보
     */
    @MessageMapping("/chat.sendMessage/{roomIdString}")
    public void sendMessage(@Payload ChatMessageDTO message, @DestinationVariable String roomIdString, Principal principal) {
        log.info("Received message for room: {}, Principal: {}", roomIdString, (principal != null ? principal.getName() : "null"));

        // 1. 발신자 정보 확인
        User sender = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + principal.getName()));

        // 2. 채팅방 정보 확인
        ChatRoom chatRoom = chatRoomRepository.findById(message.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + message.getChatRoomId()));

        // 3. 채팅 메시지 엔티티 생성 및 DB 저장
        ChatMessage chatMessage = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom)
                .content(message.getContent())
                .username(sender.getUsername())
                .timestamp(LocalDateTime.now())
                .build();

        chatMessage = chatMessageService.saveChatMessage(chatMessage); // 메시지 저장 및 unreadCount 증가
        log.info("Chat message saved. RoomId={}, Sender={}", chatRoom.getId(), sender.getUsername());

        // 4. 채팅방의 마지막 메시지 정보 업데이트
        chatRoom.setLastMessage(chatMessage.getContent());
        chatRoom.setLastMessageTime(chatMessage.getTimestamp());
        chatRoomRepository.save(chatRoom);

        // 5. 클라이언트로 전송할 DTO 정보 완성
        message.setMessageId(chatMessage.getMessageId());
        message.setSenderId(sender.getId());
        message.setUsername(sender.getUsername());
        message.setTimestamp(chatMessage.getTimestamp());
        message.setChatRoomId(chatRoom.getId());

        // --- 핵심 수정 사항 ---
        // 6. 메시지를 받을 목적지(토픽) 주소 생성
        // 예: /topic/private/1, /topic/group/12
        String destination = String.format("/topic/%s/%d",
                chatRoom.getType().toLowerCase(Locale.ROOT),
                chatRoom.getIntId());

        // 7. 해당 목적지를 구독하는 모든 클라이언트에게 메시지 브로드캐스트
        messagingTemplate.convertAndSend(destination, message);

        log.info("Message successfully broadcast to destination: {}", destination);
    }


    /**
     * 읽음 상태 처리
     * @param roomId 채팅방 ID
     * @param readStatus 읽음 처리 정보 (userId 포함)
     */
    @MessageMapping("/chat.markAsRead/{roomId}")
    public void handleMarkAsRead(@DestinationVariable String roomId, @Payload ChatReadStatusMessage readStatus) {
        try {
            chatRoomService.markMessagesAsRead(roomId, readStatus.getUserId());
            messagingTemplate.convertAndSend("/topic/readStatus/" + roomId, readStatus);
            log.info("User {} marked messages as read in room {}", readStatus.getUserId(), roomId);
        } catch (Exception e) {
            log.error("Error processing markAsRead for room {}: {}", roomId, e.getMessage(), e);
        }
    }
}
