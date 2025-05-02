package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.model.CustomUserDetails;
import ac.kr.changwon.se_proj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//로그인 시 사용자가 데이터베이스에 없으면 exception 던지는 걸 던짐
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        return userRepository.findById(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("해당 사용자 없음: " + username));
    }

}
