package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.Notification;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.NotificationRepository;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.service.SseService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;
    private final SseService sseService;
    private final TaskRepository taskRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, SseService sseService, TaskRepository taskRepository) {
        this.notificationRepository = notificationRepository;
        this.sseService = sseService;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void createNotification(User user, String message, String url) {
        // 1. 알림 객체 생성 및 DB 저장
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setUrl(url);
        notification.setRead(false);
        notificationRepository.save(notification);

        // 2. 실시간 알림 전송 (SSE)
        // 특정 사용자에게만 알림 전송
        sseService.sendToUser(user.getId(), "new-notification", notification);
    }

    /**
     * 매일 오전 8시에 실행되어 마감일이 임박한 업무에 대한 알림을 생성합니다.
     */
    @Scheduled(cron = "0 0 8 * * *") // 매일 오전 8시에 실행
    @Transactional
    public void sendDueDateNotifications() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime startOfTomorrow = tomorrow.atStartOfDay();
        LocalDateTime endOfTomorrow = tomorrow.atTime(23, 59, 59);

        List<ac.kr.changwon.se_proj.model.Task> tasksDueTomorrow = taskRepository.findByDueEndBetween(startOfTomorrow, endOfTomorrow);

        for (ac.kr.changwon.se_proj.model.Task task : tasksDueTomorrow) {
            if (task.getAssignee() != null) {
                String message = "'" + task.getProject().getProjectTitle() + "' 프로젝트의 '" + task.getTaskTitle() + "' 업무 마감이 하루 남았습니다.";
                String url = "/task?projectId=" + task.getProject().getProjectId();
                
                // 알림 생성 로직은 DB 저장과 SSE 전송을 모두 포함
                createNotification(task.getAssignee(), message, url);
            }
        }
    }
} 