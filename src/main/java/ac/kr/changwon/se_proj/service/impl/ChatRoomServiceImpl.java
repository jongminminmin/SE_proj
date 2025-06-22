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

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository;

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        List<UserChatRoom> userChatRooms = userChatRoomRepository.findByUser(user);
        return userChatRooms.stream()
                .map(userChatRoom -> {
                    ChatRoom room = userChatRoom.getChatRoom();
                    return new ChatRoomDTO(
                            room.getId(),
                            room.getIntId(),
                            room.getName(),
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        UserChatRoom userChatRoom = userChatRoomRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new RuntimeException("User is not a participant in this chat room."));

        userChatRoom.setUnreadCount(0);
        userChatRoomRepository.save(userChatRoom);
    }

    @Override
    @Transactional
    public ChatRoom createOrGetPrivateChatRoom(User user1, User user2) {
        String roomId = "private_" + (user1.getId().compareTo(user2.getId()) < 0 ? user1.getId() + "_" + user2.getId() : user2.getId() + "_" + user1.getId());
        return chatRoomRepository.findById(roomId).orElseGet(() -> {
            ChatRoom newRoom = new ChatRoom();
            newRoom.setId(roomId);
            newRoom.setName(user2.getUsername());
            newRoom.setDescription(user1.getUsername() + " and " + user2.getUsername() + "'s private chat");
            newRoom.setLastMessageTime(LocalDateTime.now());
            chatRoomRepository.save(newRoom);

            UserChatRoom ucr1 = new UserChatRoom(user1, newRoom);
            UserChatRoom ucr2 = new UserChatRoom(user2, newRoom);
            userChatRoomRepository.saveAll(Arrays.asList(ucr1, ucr2));

            return newRoom;
        });
    }

    @Override
    @Transactional
    public ChatRoom createGroupChatRoom(ChatRoomCreationRequest request, User creator) {
        ChatRoom newRoom = new ChatRoom();
        newRoom.setName(request.getName());
        newRoom.setDescription(request.getDescription());
        newRoom.setLastMessageTime(LocalDateTime.now());
        chatRoomRepository.save(newRoom);

        UserChatRoom creatorUcr = new UserChatRoom(creator, newRoom);
        userChatRoomRepository.save(creatorUcr);

        if (request.getParticipants() != null) {
            for (String participantId : request.getParticipants()) {
                User participant = userRepository.findById(participantId)
                        .orElseThrow(() -> new RuntimeException("Participant not found with ID: " + participantId));
                if (!participant.equals(creator)) {
                    UserChatRoom participantUcr = new UserChatRoom(participant, newRoom);
                    userChatRoomRepository.save(participantUcr);
                }
            }
        }
        return newRoom;
    }
}