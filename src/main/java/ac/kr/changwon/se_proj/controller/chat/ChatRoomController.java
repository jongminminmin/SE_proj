package ac.kr.changwon.se_proj.controller.chat;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO; // ChatMessageDTO 임포트
import ac.kr.changwon.se_proj.dto.ChatRoomCreationRequest;
import ac.kr.changwon.se_proj.dto.ChatRoomDTO;
import ac.kr.changwon.se_proj.model.ChatMessage; // ChatMessage 임포트
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom; // UserChatRoom 임포트
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.impl.ChatMessageServiceImpl; // NEW
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
import java.util.List;
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
    private final ChatMessageServiceImpl chatMessageService; // NEW

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
        // 이 시점에서는 DTO의 unreadCount는 0이 되어야 합니다.
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

    // 새로운 API: 특정 채팅방의 메시지 기록 조회
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

    // 새로운 API: 특정 채팅방의 메시지를 읽음 처리 (unreadCount 0으로 초기화)
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
}
