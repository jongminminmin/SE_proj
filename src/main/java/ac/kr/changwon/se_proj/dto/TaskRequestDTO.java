package ac.kr.changwon.se_proj.dto;

import java.time.LocalDate;

public class TaskRequestDTO {
    private int task_no;
    private String task_title;
    private String assignee_id;
    private String description;
    private LocalDate due_start;
    private LocalDate due_end;
}
