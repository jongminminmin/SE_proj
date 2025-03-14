package ac.kr.changwon.se_proj.Controller;

import ac.kr.changwon.se_proj.Repository.ContentRepository;
import ac.kr.changwon.se_proj.UserRepository.Content;
import ac.kr.changwon.se_proj.UserRepository.User;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;


@RestController
public class MainController {
    /* 로그인 화면 관련 컨트롤러*/
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/login")
    public User login(@RequestBody User user){
        Optional<User> searchUser = userRepository.findById(user.getId());
        if(searchUser.isPresent()){
            return ResponseEntity.ok(searchUser.get()).getBody();
        }
        else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    /*컨텐츠 관련 컨트롤러*/
    @Autowired
    private ContentRepository contentRepository;

    @PostMapping("/content")
    public Content createContent(@RequestBody Content content){
        return null;
    }
}
