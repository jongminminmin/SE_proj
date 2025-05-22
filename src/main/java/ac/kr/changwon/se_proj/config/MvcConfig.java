package ac.kr.changwon.se_proj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//
//        react라우트 부분 ->
//                registry.addViewController("/{spring:[\\w-]+}")
//                .setViewName("forward:/index.html");
//        registry.addViewController("/*/{spring:[\\w-]+}")
//                .setViewName("forward:/index.html");
//
//        // 각각의 경로에 대해, 대응하는 HTML로 포워드
//        // GET /login  → templates/login.html
//        registry.addViewController("/main").
//                setViewName("main");
//        registry.addViewController("/main.html")
//                        .setViewName("main");
//
//        registry.addViewController("/oauth2/success").
//                setViewName("login");
//
//        registry.addViewController("/login")
//                .setViewName("login");
//
//        registry.addViewController("/login.html")
//                        .setViewName("login");
//
//        registry.addViewController("/register")
//                .setViewName("register");
//        registry.addViewController("/register.html")
//                        .setViewName("register");
//        registry.addViewController("/project")
//                .setViewName("project");
//        registry.addViewController("/task")
//                .setViewName("task");
//        registry.addViewController("/chat")
//                .setViewName("chat");
//        registry.addViewController("/simpleChat")
//                .setViewName("simpleChat");
//
//
//        //웹 아이콘을 아예 무시
//        registry.addViewController("/favicon.ico").setStatusCode(HttpStatus.NO_CONTENT);
//    }
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/**") // 모든 경로에 대해
            .addResourceLocations("classpath:/static/") // src/main/resources/static/ 에서 리소스를 찾음
            .resourceChain(true)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    Resource requestedResource = location.createRelative(resourcePath);
                    // 요청된 리소스가 존재하고 읽을 수 있으면 해당 리소스를 반환
                    if (requestedResource.exists() && requestedResource.isReadable()) {
                        return requestedResource;
                    } else {
                        // 그렇지 않으면 index.html로 폴백 (SPA 라우팅 지원)
                        // index.html 파일 자체가 src/main/resources/static/ 에 있는지 확인
                        Resource indexHtml = new ClassPathResource("/static/index.html");
                        if (indexHtml.exists()) {
                            return indexHtml;
                        } else {
                            // 개발/디버깅 시 index.html이 없는 경우를 파악하기 위한 로그
                            System.err.println("중요: SPA Fallback을 위한 /static/index.html 파일을 찾을 수 없습니다!");
                            // IOException을 발생시켜 명시적으로 오류를 알릴 수도 있습니다.
                            // throw new IOException("Fallback index.html not found at classpath:/static/index.html");
                            return null; // 또는 적절한 오류 처리
                        }
                    }
                }
            });
    }
}
