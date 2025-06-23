package ac.kr.changwon.se_proj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // NoArgsConstructor 추가 (JSON 역직렬화 시 유용)

@Data
@AllArgsConstructor
@NoArgsConstructor // Lombok의 @Data와 @AllArgsConstructor만으로는 기본 생성자가 생성되지 않을 수 있습니다.
public class ChatRoomDTO {
    private String id;
    private int intId;
    private String name;
    private String type;
    private String description;
    private String lastMessage;
    private String lastMessageTime;
    private Long participants; // 참여자 수 (총 인원)
    private Integer unreadCount; // 현재 사용자 기준의 읽지 않은 메시지 수
    private String color;
}
