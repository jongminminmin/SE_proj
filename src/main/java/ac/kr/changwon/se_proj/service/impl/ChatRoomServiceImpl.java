package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService; // 인터페이스 임포트
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService { // <-- 이 부분이 핵심 수정!

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository; // NEW

    // 현재 참여 중인 채팅방 목록 가져오기 (사용자별 unreadCount 포함)
    @Override // 인터페이스 메서드임을 명시
    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRoomsByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userChatRoomRepository.findByUser(user).stream()
                .map(UserChatRoom::getChatRoom)
                .collect(Collectors.toList());
    }

    // 1:1 채팅방을 찾아 반환하거나 새로 생성하는 메서드
    @Override // 인터페이스 메서드임을 명시
    @Transactional
    public ChatRoom createOrGetPrivateChatRoom(User user1, User user2) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoomByParticipants(user1, user2);

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        } else {
            ChatRoom newRoom = ChatRoom.builder()
                    .type("PRIVATE")
                    .name(user2.getUsername()) // 1:1 방은 상대방 이름으로
                    .description("1:1 대화")
                    .color("#6c757d")
                    .lastMessage("")
                    .lastMessageTime(LocalDateTime.now())
                    .userChatRooms(new HashSet<>()) // <-- 이 부분이 핵심 수정: HashSet으로 초기화
                    .build();
            // ChatRoom의 intId를 설정 (가장 큰 intId + 1)
            Optional<ChatRoom> maxIntIdRoom = chatRoomRepository.findTopByOrderByIntIdDesc();
            newRoom.setIntId(maxIntIdRoom.map(room -> room.getIntId() + 1).orElse(1));

            // ChatRoom 저장
            newRoom = chatRoomRepository.save(newRoom);

            // UserChatRoom 엔티티 생성 및 저장 (각 사용자별 참여 정보)
            UserChatRoom ucr1 = UserChatRoom.builder()
                    .user(user1)
                    .chatRoom(newRoom)
                    .unreadCount(0) // 새 방이므로 0
                    .build();
            UserChatRoom ucr2 = UserChatRoom.builder()
                    .user(user2)
                    .chatRoom(newRoom)
                    .unreadCount(0) // 새 방이므로 0
                    .build();

            userChatRoomRepository.save(ucr1);
            userChatRoomRepository.save(ucr2);

            // ChatRoom의 userChatRooms 컬렉션에도 추가 (양방향 관계 유지)
            // 이제 newRoom.getUserChatRooms()는 null이 아니므로 add() 호출 가능
            newRoom.getUserChatRooms().add(ucr1);
            newRoom.getUserChatRooms().add(ucr2);

            return newRoom;
        }
    }

    // 특정 방의 메시지를 읽음 처리 (unreadCount 0으로 초기화 및 lastReadMessageId 업데이트)
    @Transactional
    @Override
    public void markMessagesAsRead(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + chatRoomId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        UserChatRoom userChatRoom = userChatRoomRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new RuntimeException("User is not a participant in this chat room."));

        ChatMessage lastMessage = chatRoom.getMessages() != null && !chatRoom.getMessages().isEmpty() ?
                chatRoom.getMessages().get(chatRoom.getMessages().size() - 1) : null;

        userChatRoom.setUnreadCount(0);
        userChatRoom.setLastReadMessage(lastMessage);
        userChatRoomRepository.save(userChatRoom);
    }
}
