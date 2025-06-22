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
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_no")
    private Integer taskNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "task_title", nullable = false, length = 100)
    private String taskTitle;

    private String description;

    @Column(name = "status")
    private String status;

    @Column(name = "due_start")
    private Date dueStart;

    @Column(name = "due_end")
    private Date dueEnd;

    @Lob
    @Column(name = "task_content", columnDefinition = "LONGTEXT")
    private String taskContent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

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
