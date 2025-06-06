package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "user_chat_rooms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "chat_room_id"}) // 사용자-채팅방 쌍 고유성 보장
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserChatRoom implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 참여 사용자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom; // 참여 채팅방

    @Column(name = "unread_count")
    private Integer unreadCount; // 읽지 않은 메시지 수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id") // 마지막으로 읽은 메시지 ID (NULL 허용)
    private ChatMessage lastReadMessage; // 해당 사용자가 이 방에서 마지막으로 읽은 메시지
}
