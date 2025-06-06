package ac.kr.changwon.se_proj.dto;

import lombok.Data;

@Data
public class ChatRoomCreationRequest {
    private String user1Id;
    private String user2Id;
}
