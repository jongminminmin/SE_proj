package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.ChatMessage;

import java.util.List;
import java.util.Optional;

public interface ChatMessageService {
    ChatMessage saveChatMessage(ChatMessage chatMessage);
    List<ChatMessage> getChatHistory(String chatRoomId);



    List<ChatMessage> findAll(); // 모든 메시지 조회
    Optional<ChatMessage> findById(String id); // ID로 메시지 조회 (ChatMessage ID는 String UUID)
    ChatMessage save(ChatMessage obj); // 메시지 저장/업데이트
    void deleteById(String id); // ID로 메시지 삭제 (ChatMessage ID는 String UUID)

    // void process(ChatMessage chatMessage);
}
