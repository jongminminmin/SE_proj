package ac.kr.changwon.se_proj.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알림을 수신할 사용자

    @Column(nullable = false, length = 255)
    private String message; // 알림 메시지

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 읽음 여부

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // 알림 생성 시간

    @Column
    private String url; // 클릭 시 이동할 URL (예: 특정 업무 상세 페이지)

} 