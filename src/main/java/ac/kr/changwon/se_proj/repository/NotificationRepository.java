package ac.kr.changwon.se_proj.repository;

import ac.kr.changwon.se_proj.model.Notification;
import ac.kr.changwon.se_proj.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 특정 사용자의 모든 알림을 최신순으로 조회합니다.
     * @param user 사용자 엔티티
     * @return 알림 목록
     */
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    /**
     * 특정 사용자의 읽지 않은 모든 알림을 최신순으로 조회합니다.
     * @param user 사용자 엔티티
     * @param isRead 읽음 여부 (false)
     * @return 읽지 않은 알림 목록
     */
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean isRead);
} 