package ac.kr.changwon.se_proj.dto;

import lombok.Data;
import java.util.Date;
import java.util.Set;

@Data
public class ProjectDTO {
    private int projectId;
    private String projectTitle;
    private String description;
    private UserDto owner;
    private Date date;
    private String projectMemberTier;
    private Set<UserDto> members;
} 