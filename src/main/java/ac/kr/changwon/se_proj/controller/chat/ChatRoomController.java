package ac.kr.changwon.se_proj.controller.chat;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.dto.ChatRoomCreationRequest;
import ac.kr.changwon.se_proj.dto.ChatRoomDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.impl.ChatMessageServiceImpl;
import ac.kr.changwon.se_proj.service.impl.ChatRoomServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatRoomController {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomController.class);

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomServiceImpl chatRoomService;
    private final ChatMessageServiceImpl chatMessageService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final SimpMessageSendingOperations messagingTemplate;


    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomDTO>> getMyChatRooms(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = userDetails.getUsername();
        User currentUser = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 서비스 계층을 통해 채팅방 목록을 가져옵니다.
        List<ChatRoom> rooms = chatRoomService.getChatRoomsByUserId(currentUser.getId());

        List<ChatRoomDTO> roomDTOS = rooms.stream()
                .map(room -> {
                    // 현재 사용자의 UserChatRoom 정보를 찾습니다.
                    Optional<UserChatRoom> currentUserChatRoom = room.getUserChatRooms().stream()
                            .filter(ucr -> ucr.getUser().getId().equals(currentUser.getId()))
                            .findFirst();
                    // unreadCount를 가져옵니다. 정보가 없으면 0으로 처리합니다.
                    Integer unreadCount = currentUserChatRoom.map(UserChatRoom::getUnreadCount).orElse(0);

                    return new ChatRoomDTO(
                            room.getId(),
                            room.getIntId(),
                            room.getName(),
                            room.getType(),
                            room.getDescription(),
                            room.getLastMessage(),
                            room.getLastMessageTime() != null ? room.getLastMessageTime().format(FORMATTER) : "",
                            (long) room.getUserChatRooms().size(),
                            unreadCount,
                            room.getColor()
                    );
                })
                .collect(Collectors.toList());

        log.info("사용자 {}의 채팅방 {}개 조회 완료", userId, roomDTOS.size());
        return ResponseEntity.ok(roomDTOS);
    }

    @PostMapping("/create")
    public ResponseEntity<ChatRoomDTO> createRoom(@RequestBody ChatRoomCreationRequest request, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String creatorId = userDetails.getUsername();
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + creatorId));

        ChatRoom chatRoom;

        if ("PRIVATE".equals(request.getType())) {
            if (request.getUser2Id() == null) {
                return ResponseEntity.badRequest().body(null); // .build() 대신 body(null)
            }
            User user2 = userRepository.findById(request.getUser2Id())
                    .orElseThrow(() -> new RuntimeException("User 2 not found"));
            // 서비스에 있는 동기화된 메소드를 호출하여 안전하게 채팅방을 가져오거나 생성합니다.
            chatRoom = chatRoomService.createOrGetPrivateChatRoom(creator, user2);
        } else if ("GROUP".equals(request.getType())) {
            chatRoom = chatRoomService.createGroupChatRoom(request, creator);
        } else {
            log.error("잘못된 채팅방 타입: {}", request.getType());
            return ResponseEntity.badRequest().body(null);
        }

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getId(),
                chatRoom.getIntId(),
                chatRoom.getName(),
                chatRoom.getType(),
                chatRoom.getDescription(),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageTime() != null ? chatRoom.getLastMessageTime().format(FORMATTER) : "",
                (long) chatRoom.getUserChatRooms().size(),
                0, // 처음 생성 시 읽지 않은 메시지는 0
                chatRoom.getColor()
        );

        log.info("채팅방 생성 완료: {} (타입: {})", chatRoom.getName(), request.getType());
        return ResponseEntity.ok(chatRoomDTO);
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(@PathVariable String roomId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ChatMessage> messages = chatMessageService.getChatHistory(roomId);
        List<ChatMessageDTO> messageDTOS = messages.stream()
                .map(msg -> new ChatMessageDTO(
                        msg.getMessageId(),
                        msg.getChatRoom().getId(),
                        msg.getSender().getId(),
                        msg.getUsername(),
                        msg.getContent(),
                        msg.getTimestamp()
                ))
                .collect(Collectors.toList());

        log.info("채팅방 {}의 메시지 {}개 조회 완료", roomId, messageDTOS.size());
        return ResponseEntity.ok(messageDTOS);
    }

    @PostMapping("/rooms/{roomId}/mark-as-read")
    public ResponseEntity<Void> markChatRoomAsRead(@PathVariable String roomId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = userDetails.getUsername();

        chatRoomService.markMessagesAsRead(roomId, userId);
        log.info("채팅방 {}의 메시지가 사용자 {}에 의해 읽음 처리되었습니다.", roomId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable String roomId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String deleterId = userDetails.getUsername(); // 삭제를 요청한 사용자 ID

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + roomId));

        // --- ▼▼▼ 실시간 알림 로직 시작 ▼▼▼ ---

        // 1. 방 삭제 전에 모든 참여자 목록을 가져옵니다.
        Set<User> participants = chatRoom.getUserChatRooms().stream()
                .map(UserChatRoom::getUser)
                .collect(Collectors.toSet());

        // 2. 데이터베이스에서 채팅방을 삭제합니다. (연관된 UserChatRoom, ChatMessage도 함께 삭제됩니다)
        chatRoomRepository.delete(chatRoom);
        log.info("채팅방 {} 삭제 완료 (삭제자: {})", roomId, deleterId);

        // 3. 나를 제외한 다른 참여자에게 삭제 알림을 보냅니다.
        // (1:1 채팅이므로 다른 참여자는 1명입니다)
        participants.stream()
                .filter(user -> !user.getId().equals(deleterId))
                .forEach(user -> {
                    String destination = "/topic/user/" + user.getId(); // 상대방 개인 토픽 주소
                    Map<String, String> payload = new HashMap<>();
                    payload.put("type", "ROOM_DELETED");
                    payload.put("roomId", roomId);

                    messagingTemplate.convertAndSend(destination, payload);
                    log.info("사용자 {}에게 {} 방 삭제 알림 전송", user.getId(), roomId);
                });

        // --- ▲▲▲ 실시간 알림 로직 종료 ▲▲▲ ---

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<Map<String, Object>>> getRoomParticipants(
            @PathVariable String roomId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

            List<Map<String, Object>> participants = chatRoom.getUserChatRooms().stream()
                    .map(ucr -> {
                        User user = ucr.getUser();
                        Map<String, Object> participantInfo = new HashMap<>();
                        participantInfo.put("id", user.getId());
                        participantInfo.put("username", user.getUsername());
                        participantInfo.put("email", user.getEmail());
                        participantInfo.put("avatar", "https://via.placeholder.com/32/CCCCCC/FFFFFF/?text=" +
                                user.getUsername().charAt(0));

                        return participantInfo;
                    })
                    .collect(Collectors.toList());

            log.info("채팅방 {}의 참여자 {}명 조회 완료", roomId, participants.size());
            return ResponseEntity.ok(participants);

        } catch (Exception e) {
            log.error("채팅방 {} 참여자 목록 조회 실패: {}", roomId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}