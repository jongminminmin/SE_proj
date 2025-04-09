package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/{id}")
    public User getUser(@PathVariable String id) {
        return userService.findById(id);
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        return userService.saveUser(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@RequestBody String id) {
        userService.deleteById(id);
    }

}
