package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List; // List 임포트 추가

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom implements Serializable, Persistable<String> {

    @Id
    //@GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // 채팅방의 고유 식별자 (UUID 문자열)

    @Column(name = "int_id", unique = true, nullable = false)
    private int intId; // STOMP 메시징을 위한 정수형 ID

    @Column(name = "name")
    private String name; // 채팅방 이름

    @Column(name = "description")
    private String description; // 채팅방 설명

    @Column(name = "type")
    private String type; // 채팅방 타입 (예: "PUBLIC", "PRIVATE", "GROUP")

    @Column(name = "last_message")
    private String lastMessage; // 마지막 메시지 내용

    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime; // 마지막 메시지 전송 시간

    @Column(name = "color")
    private String color; // 채팅방 UI에 사용될 색상

    @Version
    private Long version;

    // ChatRoom과 UserChatRoom 간의 One-to-Many 관계 설정
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserChatRoom> userChatRooms = new HashSet<>();

    // ChatRoom과 ChatMessage 간의 One-to-Many 관계 설정 (메시지 이력)
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages; // 이 방의 메시지 목록

    // 편의 메서드 (빌더 패턴과 AllArgsConstructor 사용 시 필수적이지는 않음)
    // 기존 participants 관련 메서드는 이제 UserChatRoom을 통해 관리되므로 제거

    @Override
    @Transient
    public boolean isNew(){
        return this.version == null;
    }
}
