package ac.kr.changwon.se_proj.dto;

public class TaskRequestDTO {
    private String taskTitle;
    private String description;
    private String status;
    private String dueEnd;
    private Long projectId;
    private String assigneeId;

    // Getters
    public String getTaskTitle() {
        return taskTitle;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getDueEnd() {
        return dueEnd;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    // Setters
    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDueEnd(String dueEnd) {
        this.dueEnd = dueEnd;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }
}
