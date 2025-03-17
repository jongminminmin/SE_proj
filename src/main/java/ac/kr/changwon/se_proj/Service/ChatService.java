package ac.kr.changwon.se_proj.Service;

import ac.kr.changwon.se_proj.Repository.ContentRepository;
import lombok.Data;

import java.net.DatagramSocket;


@Data
public class ChatService {

    private final int port=5000;
    private DatagramSocket socket;
    private ContentRepository contentRepository;



}
