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
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;
    private final ChatProperties chatProps;
    private final ChatRoomRepository chatRoomRepository; // NEW
    private final ChatMessageServiceImpl chatMessageService; // NEW
    private final ChatRoomService chatRoomService;

    /**
     * WebSocket 메시지 수신 처리
     */
    @MessageMapping("/chat.sendMessage/{roomIdString}")
    public void sendMessage(@Payload ChatMessageDTO message, @DestinationVariable String roomIdString, Principal principal) {
        log.info("Received message in sendMessage for room PathVariable: {}, DTO_RoomID: {}, Principal: {}",
                roomIdString, // 경로에서 추출한 방 ID (예: "project_001")
                (message != null ? message.getChatRoomId() : "null_DTO_roomId"), // DTO에 포함된 방 ID (String 타입이어야 함)
                (principal != null ? principal.getName() : "null_principal"));

        String usernameFromClient = (principal != null) ? principal.getName() : message.getUsername();
        log.info("Username to find in repository: {}", usernameFromClient);

        User sender = userRepository.findByUsername(usernameFromClient)
                .orElseThrow(() -> {
                    log.error("User not found for username: {}. Throwing UsernameNotFoundException.", usernameFromClient);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + usernameFromClient);
                });
        log.info("Sender found: {}", sender.getUsername());

        ChatRoom chatRoom = chatRoomRepository.findById(message.getChatRoomId()) // DTO의 chatRoomId 사용
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + message.getChatRoomId()));

        // DB 저장 (ChatMessageServiceImpl로 위임)
        ChatMessage chat = ChatMessage.builder()
                .sender(sender)
                .chatRoom(chatRoom) // ChatRoom 객체 연결
                .content(Objects.requireNonNull(message).getContent())
                .username(sender.getUsername()) // 실제 발신자 username으로 설정
                .timestamp(LocalDateTime.now()) // 현재 서버 시간으로 설정
                .build();

        // 메시지 저장 및 다른 참여자의 unreadCount 증가
        chat = chatMessageService.saveChatMessage(chat);
        log.info("Chat message saved to DB and unread counts updated: RoomId={}, Sender={}", chat.getChatRoom().getId(), chat.getSender().getUsername());

        // ChatRoom의 lastMessage와 lastMessageTime 업데이트
        chatRoom.setLastMessage(chat.getContent());
        chatRoom.setLastMessageTime(chat.getTimestamp());
        chatRoomRepository.save(chatRoom); // ChatRoom 업데이트 저장

        // 클라이언트로 보낼 DTO 업데이트 (저장된 메시지의 ID와 타임스탬프 사용)
        message.setMessageId(chat.getMessageId()); // 저장된 메시지의 ID 설정
        message.setSenderId(sender.getId()); // 발신자 ID 설정
        message.setUsername(sender.getUsername());
        message.setTimestamp(chat.getTimestamp()); // DB에 저장된 시간으로 통일
        message.setChatRoomId(chatRoom.getId()); // ChatRoom ID 설정

        //분기 설정. roomId필드 기준으로 Private, Group 구분
        String destination;
        if(chatRoom.getIntId() <= chatProps.getPrivateMaxRoomId()){ // ChatRoom 객체에서 intId 사용
            destination = "/topic/private/" + chatRoom.getIntId();
        }
        else{
            destination = "/topic/group/" + chatRoom.getIntId();
        }
        log.info("Calculated destination: {}", destination);

        try {
            messagingTemplate.convertAndSend(destination, message);
            log.info("Message sent to destination: {}, Payload Content: {}", destination, message.getContent());
        } catch (Exception e) {
            log.error("Error sending message via messagingTemplate to {}. Error: {}", destination, e.getMessage(), e);
        }
    }

    // 읽음 상태 처리 핸들러 추가
    @MessageMapping("/chat.markAsRead/{roomId}")
    public void handleMarkAsRead(@DestinationVariable String roomId, ChatReadStatusMessage readStatus) {
        try {
            // 1. 백엔드에서 읽음 처리 수행
            chatRoomService.markMessagesAsRead(roomId, readStatus.getUserId());

            // 2. 읽음 상태를 해당 채팅방 구독자들에게 브로드캐스트
            messagingTemplate.convertAndSend("/topic/readStatus/" + roomId, readStatus);

            log.info("사용자 {}가 채팅방 {}에서 읽음 처리 완료", readStatus.getUserId(), roomId);
        } catch (Exception e) {
            log.error("읽음 상태 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
