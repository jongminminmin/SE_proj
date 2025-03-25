package ac.kr.changwon.se_proj.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roomId;

    @Column(nullable = false)
    private String userId;

    private String content;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(columnDefinition = "DATE DEFAULT CURRENT_DATE")
    private Date timestamp;

    @Column(unique = true)
    private String fileData;
}
