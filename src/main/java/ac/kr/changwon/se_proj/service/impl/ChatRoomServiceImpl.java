package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.ChatRoomCreationRequest;
import ac.kr.changwon.se_proj.dto.ChatRoomDTO;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomServiceImpl.class);

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository;

    /**
     * 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
     * 이제 ChatRoom 엔티티가 Persistable을 구현하므로, 가장 표준적인 코드로 동시성 문제가 해결됩니다.
     * @param user1 사용자 1
     * @param user2 사용자 2
     * @return 생성되거나 조회된 채팅방
     */
    @Override
    @Transactional
    public ChatRoom createOrGetPrivateChatRoom(User user1, User user2) {
        final String roomId = "private_" + (user1.getId().compareTo(user2.getId()) < 0 ? user1.getId() + "_" + user2.getId() : user2.getId() + "_" + user1.getId());

        return chatRoomRepository.findById(roomId).orElseGet(() -> {
            log.info("채팅방 {}이(가) 존재하지 않으므로 새로 생성합니다.", roomId);

            ChatRoom newRoom = new ChatRoom();
            newRoom.setId(roomId); // ID를 직접 설정
            newRoom.setName(user1.getUsername() + ", " + user2.getUsername());
            newRoom.setDescription(user1.getUsername() + " and " + user2.getUsername() + "'s private chat");
            newRoom.setLastMessageTime(LocalDateTime.now());
            newRoom.setType("PRIVATE");
            newRoom.setIntId(Math.abs(roomId.hashCode()));

            // isNew() 메소드 덕분에 JPA가 이 객체를 '새로운 것'으로 정확히 인지하고 처리합니다.
            ChatRoom savedRoom = chatRoomRepository.save(newRoom);

            UserChatRoom ucr1 = new UserChatRoom(user1, savedRoom);
            UserChatRoom ucr2 = new UserChatRoom(user2, savedRoom);
            userChatRoomRepository.saveAll(Arrays.asList(ucr1, ucr2));

            return savedRoom;
        });
    }

    // ... (이하 다른 메소드들은 수정 없음) ...

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoom> getChatRoomsByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        return userChatRoomRepository.findByUser(user).stream()
                .map(UserChatRoom::getChatRoom)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> findUserChatRooms(String userId) {
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + userId));
        List<UserChatRoom> userChatRooms = userChatRoomRepository.findByUser(user);
        return userChatRooms.stream()
                .map(userChatRoom -> {
                    ChatRoom room = userChatRoom.getChatRoom();
                    return new ChatRoomDTO(
                            room.getId(),
                            room.getIntId(),
                            room.getName(),
                            room.getType(),
                            room.getDescription(),
                            room.getLastMessage(),
                            room.getLastMessageTime() != null ? room.getLastMessageTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "",
                            (long) room.getUserChatRooms().size(),
                            userChatRoom.getUnreadCount(),
                            room.getColor()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(String chatRoomId, String userId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + chatRoomId));
        User user = userRepository.findByUsername(userId)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + userId));

        UserChatRoom userChatRoom = userChatRoomRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new RuntimeException("User is not a participant in this chat room."));

        userChatRoom.setUnreadCount(0);
        userChatRoomRepository.save(userChatRoom);
    }

    @Override
    @Transactional
    public ChatRoom createGroupChatRoom(ChatRoomCreationRequest request, User creator) {
        ChatRoom newRoom = new ChatRoom();
        newRoom.setName(request.getName());
        newRoom.setDescription(request.getDescription());
        newRoom.setLastMessageTime(LocalDateTime.now());

        ChatRoom savedRoom = chatRoomRepository.save(newRoom);

        UserChatRoom creatorUcr = new UserChatRoom(creator, savedRoom);
        userChatRoomRepository.save(creatorUcr);

        if (request.getParticipants() != null) {
            for (String participantId : request.getParticipants()) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> new RuntimeException("Participant not found with ID: " + participantId));
                if (!participant.equals(creator)) {
                    UserChatRoom participantUcr = new UserChatRoom(participant, savedRoom);
                    userChatRoomRepository.save(participantUcr);
                }
            }
        }
        return savedRoom;
    }
}
