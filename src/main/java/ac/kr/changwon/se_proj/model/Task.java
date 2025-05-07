package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.Date;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int taskNo;

    @JoinColumn(name = "project_id")
    private Integer projectId; // FK to Project.project_id

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "task_title", nullable = false, length = 100)
    private String taskTitle;

    private String description;

    @Column(name = "due_start")
    private Date dueStart;

    @Column(name = "due_end")
    private Date dueEnd;

    @Lob
    @Column(name = "task_content", columnDefinition = "LONGTEXT")
    private String taskContent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt  = LocalDateTime.now();
    }

    public String getDDay() {
        if (dueEnd == null) return "";
        long days = ChronoUnit.DAYS.between((Temporal) LocalDateTime.now(), (Temporal) dueEnd);
        return days >= 0 ? "D-" + days : "남음";
    } //D-Day

    public boolean isDue() {
        return dueEnd != null && dueEnd.equals(LocalDate.now().plusDays(1));
    }//마감일 하루 전인지 확인

    //@Enumerated(EnumType.STRING)

}
