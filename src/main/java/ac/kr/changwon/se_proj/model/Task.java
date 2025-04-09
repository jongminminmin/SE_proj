package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String projectTitle; // FK to Project.projectTitle

    @ManyToOne
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(nullable = false)
    private String taskTitle;

    private String description;

    @Temporal(TemporalType.DATE)
    private Date dueStart;

    @Temporal(TemporalType.DATE)
    private Date dueEnd;
}
