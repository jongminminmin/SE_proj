package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Repository.ContentRepository;
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

    @PostMapping("/content")
    public Content createContent(@RequestBody Content content){
        return null;
    }
}
