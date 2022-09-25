package cn.com.spotty.hos;

import cn.com.spotty.hos.mybatis.HosDataSourceConfig;
import cn.com.spotty.hos.security.SecurityInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;


@EnableWebMvc
@SuppressWarnings("deprecation")
@Configuration
@ComponentScan({"cn.com.spotty.hos.**"})
@SpringBootApplication(exclude = MongoAutoConfiguration.class)
@Import({HosDataSourceConfig.class, HosServerBeanConfiguration.class})
@MapperScan("cn.com.spotty.hos")
public class HosServerApp {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityInterceptor securityInterceptor;

    @Bean
    public WebMvcConfigurer configurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOrigins("*");
            }

            @Override
            public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                registry.addInterceptor(securityInterceptor);
            }
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(HosServerApp.class, args);
    }
}
