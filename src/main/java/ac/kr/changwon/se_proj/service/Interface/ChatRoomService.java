package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.dto.ChatRoomCreationRequest;
import ac.kr.changwon.se_proj.dto.ChatRoomDTO;
import ac.kr.changwon.se_proj.model.ChatRoom;
import ac.kr.changwon.se_proj.model.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ChatRoomService {
    List<ChatRoom> getChatRoomsByUserId(String userId);
    ChatRoom createOrGetPrivateChatRoom(User user1, User user2);
    void markMessagesAsRead(String chatRoomId, String userId);// markMessagesAsRead 메서드도 인터페이스에 추가
    ChatRoom createGroupChatRoom(ChatRoomCreationRequest request, User creator);
    List<ChatRoomDTO> findUserChatRooms(String userId);
}
