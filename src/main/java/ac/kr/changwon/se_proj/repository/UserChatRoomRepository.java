package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.model.UserChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, String> {
    // 특정 사용자와 특정 채팅방에 대한 UserChatRoom 엔티티를 조회합니다.
    Optional<UserChatRoom> findByUserAndChatRoom(User user, ChatRoom chatRoom);
    List<UserChatRoom> findByUser(User user);

}
