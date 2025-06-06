package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.ChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserChatRoomRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final UserChatRoomRepository userChatRoomRepository;

    @Override
    @Transactional
    public ChatMessage saveChatMessage(ChatMessage chatMessage) {
        // 메시지 저장 전에 ChatRoom을 현재 트랜잭션 컨텍스트로 다시 로드하거나 가져옵니다.
        // 이렇게 하면 userChatRooms 컬렉션에 접근할 때 Session이 활성화되어 있습니다.
        ChatMessage finalChatMessage = chatMessage;
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessage.getChatRoom().getId()) // ChatRoom ID를 사용하여 다시 로드
                .orElseThrow(() -> new RuntimeException("ChatRoom not found during message saving: " + finalChatMessage.getChatRoom().getId()));

        chatMessage.setChatRoom(chatRoom); // 다시 로드한 ChatRoom 객체를 메시지에 설정

        chatMessage = chatMessageRepository.save(chatMessage);

        User sender = chatMessage.getSender();

        // userChatRooms 컬렉션에 접근 (이제 트랜잭션 내에서 로드되었으므로 문제 없음)
        Set<UserChatRoom> userChatRooms = chatRoom.getUserChatRooms();
        for (UserChatRoom ucr : userChatRooms) {
            if (!ucr.getUser().equals(sender)) {
                ucr.setUnreadCount(ucr.getUnreadCount() + 1);
                userChatRoomRepository.save(ucr);
            }
        }
        return chatMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(String chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("ChatRoom not found with ID: " + chatRoomId));
        return chatMessageRepository.findByChatRoomOrderByTimestampAsc(chatRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> findAll() {
        return chatMessageRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatMessage> findById(String id) { // ID 타입 String
        return chatMessageRepository.findById(id); // Integer.valueOf(id) 제거
    }

    @Override
    @Transactional
    public ChatMessage save(ChatMessage chatMessage) { // saveChatMessage와 동일하게 처리
        return saveChatMessage(chatMessage);
    }

    @Override
    @Transactional
    public void deleteById(String id) { // ID 타입 String
        chatMessageRepository.deleteById(id); // Integer.valueOf(id) 제거
    }

    // process 메서드가 인터페이스에 있다면 여기에 구현
    // @Override
    // public void process(ChatMessage chatMessage) {
    //     System.out.println("Processing ChatMessage (from CRUD controller): " + chatMessage.getMessageId());
    // }
}
