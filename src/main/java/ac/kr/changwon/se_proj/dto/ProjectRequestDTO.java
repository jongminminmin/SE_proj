package ac.kr.changwon.se_proj.dto;

import lombok.Getter;
import lombok.Setter;


import java.time.LocalDate;
@Getter
@Setter
public class ProjectRequestDTO {
    private int project_id;
    private String project_title;
    private String description;
    private String owner_id;
    private LocalDate date;
    private String project_member_tier;

}
