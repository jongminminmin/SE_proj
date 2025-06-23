package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.CommentDTO;
import ac.kr.changwon.se_proj.model.Comment;
import ac.kr.changwon.se_proj.model.Task;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.CommentRepository;
import ac.kr.changwon.se_proj.repository.TaskRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(CommentRepository commentRepository, TaskRepository taskRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByTaskId(Integer taskId) {
        return commentRepository.findByTask_TaskNoOrderByCreatedAtAsc(taskId).stream()
                .map(CommentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDTO createComment(Integer taskId, String content, String authorId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        Comment comment = new Comment();
        comment.setTask(task);
        comment.setAuthor(author);
        comment.setContent(content);

        Comment savedComment = commentRepository.save(comment);
        return CommentDTO.fromEntity(savedComment);
    }

    @Override
    @Transactional
    public CommentDTO updateComment(Long commentId, String content, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("You do not have permission to edit this comment.");
        }

        comment.setContent(content);
        Comment updatedComment = commentRepository.save(comment);
        return CommentDTO.fromEntity(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String currentUsername) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("You do not have permission to delete this comment.");
        }

        commentRepository.delete(comment);
    }
}