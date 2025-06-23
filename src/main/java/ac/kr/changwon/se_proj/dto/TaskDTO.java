package ac.kr.changwon.se_proj.dto;

import lombok.Data;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class TaskDTO {
    private Integer taskNo;
    private Long projectId;
    private String taskTitle;
    private String description;
    private String status;
    private UserDto assignee;
    private String dueEnd;

    // 삭제된 태스크를 위한 필드
    private Integer deletedTaskId;

    public TaskDTO(Integer deletedTaskId) {
        this.deletedTaskId = deletedTaskId;
    }
} 