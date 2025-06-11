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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomDTO>> getMyChatRooms(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = userDetails.getUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ChatRoom> rooms = chatRoomService.getChatRoomsByUserId(currentUser.getId());

        List<ChatRoomDTO> roomDTOS = rooms.stream()
                .map(room -> {
                    // 현재 사용자 기준의 unreadCount를 UserChatRoom에서 가져옵니다.
                    Optional<UserChatRoom> currentUserChatRoom = room.getUserChatRooms().stream()
                            .filter(ucr -> ucr.getUser().getId().equals(currentUser.getId()))
                            .findFirst();
                    Integer unreadCount = currentUserChatRoom.map(UserChatRoom::getUnreadCount).orElse(0);

                    return new ChatRoomDTO(
                            room.getId(),
                            room.getIntId(),
                            room.getName(),
                            room.getDescription(),
                            room.getLastMessage(),
                            room.getLastMessageTime() != null ? room.getLastMessageTime().format(FORMATTER) : "",
                            (long) room.getUserChatRooms().size(), // 참여자 수는 UserChatRoom의 개수로 계산
                            unreadCount, // 현재 사용자 기준의 unreadCount
                            room.getColor()
                    );
                })
                .collect(Collectors.toList());

        log.info("사용자 {}의 채팅방 {}개 조회 완료", username, roomDTOS.size());
        return ResponseEntity.ok(roomDTOS);
    }

    @PostMapping("/create-private-room")
    public ResponseEntity<ChatRoomDTO> createOrGetPrivateChatRoom(@RequestBody ChatRoomCreationRequest request) {
        log.info("1:1 채팅방 생성/입장 요청: user1Id={}, user2Id={}", request.getUser1Id(), request.getUser2Id());
        User user1 = userRepository.findById(request.getUser1Id())
                .orElseThrow(() -> new RuntimeException("User 1 not found"));
        User user2 = userRepository.findById(request.getUser2Id())
                .orElseThrow(() -> new RuntimeException("User 2 not found"));

        ChatRoom chatRoom = chatRoomService.createOrGetPrivateChatRoom(user1, user2);

        // 1:1 방 생성/입장 시, 본인의 unreadCount를 0으로 초기화
        chatRoomService.markMessagesAsRead(chatRoom.getId(), user1.getId()); // user1이 현재 사용자라고 가정

        // 생성되거나 찾아진 채팅방 정보를 DTO로 변환하여 반환
        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getId(),
                chatRoom.getIntId(),
                chatRoom.getName(),
                chatRoom.getDescription(),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageTime() != null ? chatRoom.getLastMessageTime().format(FORMATTER) : "",
                (long) chatRoom.getUserChatRooms().size(), // 참여자 수는 UserChatRoom의 개수로 계산
                0, // 새로 입장했으므로 unreadCount는 0으로 설정
                chatRoom.getColor()
        );

        return ResponseEntity.ok(chatRoomDTO);
    }

    @PostMapping("/create-room")
    public ResponseEntity<ChatRoomDTO> createRoom(@RequestBody ChatRoomCreationRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = userDetails.getUsername();
        User creator = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom;

        // type에 따른 분기 처리
        if ("PRIVATE".equals(request.getType())) {
            // 1:1 채팅방 생성 (기존 로직 활용)
            if (request.getUser2Id() == null) {
                return ResponseEntity.badRequest().build();
            }
            User user2 = userRepository.findById(request.getUser2Id())
                    .orElseThrow(() -> new RuntimeException("User 2 not found"));
            chatRoom = chatRoomService.createOrGetPrivateChatRoom(creator, user2);
        } else if ("GROUP".equals(request.getType())) {
            // 그룹 채팅방 생성 (새로운 로직 필요)
            chatRoom = chatRoomService.createGroupChatRoom(request, creator);
        } else {
            log.error("잘못된 채팅방 타입: {}", request.getType());
            return ResponseEntity.badRequest().build();
        }

        // DTO 변환 후 반환
        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getId(),
                chatRoom.getIntId(),
                chatRoom.getName(),
                chatRoom.getDescription(),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageTime() != null ? chatRoom.getLastMessageTime().format(FORMATTER) : "",
                (long) chatRoom.getUserChatRooms().size(),
                0, // 새로 생성했으므로 unreadCount는 0
                chatRoom.getColor()
        );

        log.info("채팅방 생성 완료: {} (타입: {})", chatRoom.getName(), request.getType());
        return ResponseEntity.ok(chatRoomDTO);
    }

    // 특정 채팅방의 메시지 기록 조회
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDTO>> getChatMessages(@PathVariable String roomId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
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

    // 특정 채팅방의 메시지를 읽음 처리 (unreadCount 0으로 초기화)
    @PostMapping("/rooms/{roomId}/mark-as-read")
    public ResponseEntity<Void> markChatRoomAsRead(@PathVariable String roomId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();

        chatRoomService.markMessagesAsRead(roomId, userId);
        log.info("채팅방 {}의 메시지가 사용자 {}에 의해 읽음 처리되었습니다.", roomId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(@PathVariable String roomId, @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = userDetails.getUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 권한 체크 (예: 방장만 삭제 가능하도록 하려면 추가 로직 필요)
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        // 실제 서비스에서는 추가 권한 체크 필요
        // 예: if (!chatRoom.getOwner().equals(currentUser)) { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

        chatRoomRepository.delete(chatRoom);
        log.info("채팅방 {} 삭제 완료 (삭제자: {})", roomId, username);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<Map<String, Object>>> getRoomParticipants(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

            // UserChatRoom을 통해 참여자 목록 가져오기
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
