package ac.kr.changwon.se_proj.dto;

import lombok.Data;
import java.util.Date;

@Data
public class TaskDTO {
    private int taskNo;
    private Integer projectId;
    private UserDto assignee;
    private String taskTitle;
    private String description;
    private Date dueStart;
    private Date dueEnd;
    private String taskContent;
} 