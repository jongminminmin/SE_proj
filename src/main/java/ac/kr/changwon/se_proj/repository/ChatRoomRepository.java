package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    // 특정 User가 참여하는 모든 ChatRoom을 찾습니다 (UserChatRoom 조인)
    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.userChatRooms ucr WHERE ucr.user = :user")
    List<ChatRoom> findByParticipantsContaining(@Param("user") User user);

    // 두 특정 사용자가 참여하는 'PRIVATE' 타입의 채팅방을 찾습니다.
    // UserChatRoom 엔티티를 통해 관계를 탐색합니다.
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
            "JOIN cr.userChatRooms ucr1 JOIN cr.userChatRooms ucr2 " +
            "WHERE ucr1.user = :user1 AND ucr2.user = :user2 AND cr.type = 'PRIVATE' AND ucr1 <> ucr2")
    Optional<ChatRoom> findPrivateChatRoomByParticipants(@Param("user1") User user1, @Param("user2") User user2);

    // 가장 큰 intId를 가진 ChatRoom을 찾는 메서드
    Optional<ChatRoom> findTopByOrderByIntIdDesc();
}
