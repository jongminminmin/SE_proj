package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;

import java.util.List;

public interface ChatRoomService {
    List<ChatMessageDTO> getRoomByUserId(String userId);
}
