package ac.kr.changwon.se_proj.dto;

import ac.kr.changwon.se_proj.model.Comment;
import lombok.Builder;
import lombok.Data;
import java.util.Date;

@Data
@Builder
public class CommentDTO {
    private Long id;
    private String content;
    private Date createdAt;
    private UserDto author;

    public static CommentDTO fromEntity(Comment comment) {
        return CommentDTO.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .author(UserDto.fromEntity(comment.getAuthor()))
                .build();
    }
} 