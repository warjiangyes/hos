package cn.com.spotty.hos.server.test;

import org.junit.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import java.io.IOException;

public class PathTest {
    @Test
    public void getPath() {
        try {
            DefaultResourceLoader loader = new DefaultResourceLoader();
            Resource resource = loader.getResource("classpath:hadoop/core-site.xml");
            System.out.println(resource.getFile().getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
