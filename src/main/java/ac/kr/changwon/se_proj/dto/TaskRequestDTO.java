package ac.kr.changwon.se_proj.dto;

import java.time.LocalDate;

public class TaskRequestDTO {
    private int task_no;
    private String taskTitle;
    private String assigneeId;
    private String description;
    private LocalDate dueStart;
    private LocalDate dueEnd;
}
