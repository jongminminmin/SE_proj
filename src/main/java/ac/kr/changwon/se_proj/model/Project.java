package ac.kr.changwon.se_proj.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    private String projectTitle;
    private String description;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    @Temporal(TemporalType.DATE)
    private Date date;

    @Column(name = "project_member_tier", nullable = false)
    private String projectMemberTier;

    //프로젝트 멤버(다대다관계)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_members",
            joinColumns = @JoinColumn(name ="project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();


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
