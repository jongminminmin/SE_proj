package ac.kr.changwon.se_proj.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRoomCreationRequest {
    private String user1Id;
    private String user2Id;

    //그룹 채팅방용 필드
    private String name;
    private String description;
    private String type;
    private String project;
    private List<String> participants;
}
