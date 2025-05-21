package ac.kr.changwon.se_proj.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int project_id;

    private String projectTitle;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Temporal(TemporalType.DATE)
    private Date date;

    @Column(nullable = false)
    private String projectMemberTier;

    /*
    public Project(int projectId, String projectTitle, String description, User owner, Date date, String projectMemberTier) {
        this.project_id = projectId;
        this.projectTitle = projectTitle;
        this.description = description;
        this.owner = owner;
        this.projectMemberTier = projectMemberTier;
        this.date = date;
    }
    */
    public Project(int projectId, String projectTitle, String description, String ownerId, LocalDate date, String projectMemberTier) {
    }
}

/* 프로젝트 생성 시
* Project p=new Project
* p.setOwner(adminUser)*/
