package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "chat_messages") // 테이블 이름 변경
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // UUID로 ID 자동 생성
    @Column(name = "message_id", updatable = false, nullable = false)
    private String messageId; // 메시지 고유 ID (UUID 문자열)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false) // chat_rooms 테이블의 id를 참조
    private ChatRoom chatRoom; // 메시지가 속한 채팅방

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false) // users 테이블의 id를 참조
    private User sender; // 발신자 User 객체

    @Column(name = "username", nullable = false)
    private String username; // 발신자 사용자 이름 (편의상 중복 저장)

    @Column(name = "content", length = 1000, nullable = false) // 메시지 내용 (길이 증가)
    private String content;

    @Column(name = "timestamp")
    private LocalDateTime timestamp; // 메시지 전송 시간

    // ReceiverId는 1:1 채팅방에서 ChatRoom 자체가 수신자를 암시하므로 제거
    // int roomId는 ChatRoom 객체를 통해 접근하므로 제거
}
