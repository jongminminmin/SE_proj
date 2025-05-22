package ac.kr.changwon.se_proj.dto;

import lombok.Getter;
import lombok.Setter;


import java.time.LocalDate;
@Getter
@Setter
public class ProjectRequestDTO {
    private int projectId;
    private String projectTitle;
    private String description;
    private String ownerId;
    private LocalDate date;
    private String projectMemberTier;

}
