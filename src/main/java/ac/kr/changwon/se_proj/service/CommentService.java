package ac.kr.changwon.se_proj.service;

import ac.kr.changwon.se_proj.dto.CommentDTO;
import java.util.List;

public interface CommentService {
    List<CommentDTO> getCommentsByTaskId(Integer taskId);
    CommentDTO createComment(Integer taskId, String content, String authorId);
    CommentDTO updateComment(Long commentId, String content, String currentUsername);
    void deleteComment(Long commentId, String currentUsername);
} 