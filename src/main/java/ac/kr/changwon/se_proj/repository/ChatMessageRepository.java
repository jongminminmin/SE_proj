package ac.kr.changwon.se_proj.repository;


import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    // 특정 채팅방의 메시지 목록을 시간 순서대로 조회 (오래된 것부터)
    List<ChatMessage> findByChatRoomOrderByTimestampAsc(ChatRoom chatRoom);
}
