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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    public ResponseEntity<List<ChatRoomDTO>> getMyChatRooms(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String userId = userDetails.getUsername();
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<ChatRoom> rooms = chatRoomService.getChatRoomsByUserId(currentUser.getId());

        List<ChatRoomDTO> roomDTOS = rooms.stream()
                .map(room -> {
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
                            (long) room.getUserChatRooms().size(),
                            unreadCount,
                            room.getColor()
                    );
                })
                .collect(Collectors.toList());

        log.info("사용자 {}의 채팅방 {}개 조회 완료", userId, roomDTOS.size());
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

        chatRoomService.markMessagesAsRead(chatRoom.getId(), user1.getId());

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getId(),
                chatRoom.getIntId(),
                chatRoom.getName(),
                chatRoom.getDescription(),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageTime() != null ? chatRoom.getLastMessageTime().format(FORMATTER) : "",
                (long) chatRoom.getUserChatRooms().size(),
                0,
                chatRoom.getColor()
        );

        return ResponseEntity.ok(chatRoomDTO);
    }

    @PostMapping("/create")
    public ResponseEntity<ChatRoomDTO> createRoom(@RequestBody ChatRoomCreationRequest request, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String creatorId = userDetails.getUsername();
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + creatorId));

        List<User> members = new ArrayList<>();
        for (String memberId : request.getParticipants()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + memberId));
            members.add(member);
        }

        ChatRoom chatRoom;

        if ("PRIVATE".equals(request.getType())) {
            if (request.getUser2Id() == null) {
                return ResponseEntity.badRequest().build();
            }
            User user2 = userRepository.findById(request.getUser2Id())
                    .orElseThrow(() -> new RuntimeException("User 2 not found"));
            chatRoom = chatRoomService.createOrGetPrivateChatRoom(creator, user2);
        } else if ("GROUP".equals(request.getType())) {
            chatRoom = chatRoomService.createGroupChatRoom(request, creator);
        } else {
            log.error("잘못된 채팅방 타입: {}", request.getType());
            return ResponseEntity.badRequest().build();
        }

        ChatRoomDTO chatRoomDTO = new ChatRoomDTO(
                chatRoom.getId(),
                chatRoom.getIntId(),
                chatRoom.getName(),
                chatRoom.getDescription(),
                chatRoom.getLastMessage(),
                chatRoom.getLastMessageTime() != null ? chatRoom.getLastMessageTime().format(FORMATTER) : "",
                (long) chatRoom.getUserChatRooms().size(),
                0,
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
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

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
        String userId = userDetails.getUsername();
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found"));

        chatRoomRepository.delete(chatRoom);
        log.info("채팅방 {} 삭제 완료 (삭제자: {})", roomId, userId);

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
