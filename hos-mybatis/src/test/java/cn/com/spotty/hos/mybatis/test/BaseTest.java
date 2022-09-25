package cn.com.spotty.hos.mybatis.test;

import cn.com.spotty.hos.mybatis.HosDataSourceConfig;
import org.junit.runner.RunWith;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(HosDataSourceConfig.class)
@PropertySource("classpath:application.properties")
@ComponentScan("cn.com.spotty.hos.**")
@MapperScan("cn.com.spotty.hos.**")
public class BaseTest {
    public BaseTest() {
        System.out.println("BaseTest");
    }
}
