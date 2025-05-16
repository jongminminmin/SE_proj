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

    public void addResourceHandler(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/**")//모든 패턴에 대해 일치
                .addResourceLocations("classpath:/static/")//resource/static에 있는 static디렉터리 서빙
                .resourceChain(true)
                //SPA (Single Page Application) fallback 로직
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if(requestedResource.exists() && requestedResource.isReadable()){
                            return requestedResource;
                        }
                        else {
                            return new ClassPathResource("/static/index.html");
                        }
                    }
                });
    }


}
