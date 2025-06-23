package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
// UserChatRoom의 기본 키(ID) 타입이 Long이라면 <UserChatRoom, Long>으로 지정해야 합니다.
// 만약 String(UUID)이라면 현재 코드가 맞습니다.
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, String> {

    Optional<UserChatRoom> findByUserAndChatRoom(User user, ChatRoom chatRoom);
    List<UserChatRoom> findByUser(User user);

    /**
     * 네이티브 쿼리를 사용하여 user_chat_rooms 테이블을 직접 업데이트합니다.
     * 이 방식은 JPQL보다 더 명확하고 안정적으로 동작합니다.
     * @param lastMessageId 이 파라미터의 타입은 ChatMessage의 messageId 타입과 일치해야 합니다. (Long)
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE user_chat_rooms SET unread_count = 0, last_read_message_id = :lastMessageId WHERE chat_room_id = :chatRoomId AND user_id = :userId",
            nativeQuery = true)
    void resetUnreadCountAndSetLastReadMessage(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId, @Param("lastMessageId") String lastMessageId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE user_chat_rooms SET unread_count = 0 WHERE chat_room_id = :chatRoomId AND user_id = :userId",
            nativeQuery = true)
    void resetUnreadCount(@Param("chatRoomId") String chatRoomId, @Param("userId") String userId);
}
