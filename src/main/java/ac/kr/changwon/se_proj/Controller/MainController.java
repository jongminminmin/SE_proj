package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Repository.ChatRepository;
import ac.kr.changwon.se_proj.Repository.ContentRepository;
import ac.kr.changwon.se_proj.Service.impl.ChatServiceImpl;
import ac.kr.changwon.se_proj.UserRepository.Content;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MainController {


    /*컨텐츠 관련 컨트롤러*/
    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private ChatServiceImpl chatServiceImpl;

    @PostMapping("/index")
    public Content createContent(@RequestBody Content content){
        return contentRepository.save(content);
    }

    @Autowired
    private ChatRepository chatRepository;

    @PostMapping("/simpleChatService")
    public String handleChatService(@RequestBody String message){
       try {
           chatServiceImpl.sendMessage(message);
           return "success";
       }
       catch (Exception e){
            System.err.println("Error sending chat message"+e.getMessage());
            return "error";
       }
    }
}
