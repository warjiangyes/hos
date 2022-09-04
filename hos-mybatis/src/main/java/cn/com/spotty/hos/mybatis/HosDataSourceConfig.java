package cn.com.spotty.hos.mybatis;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

@Configuration
@MapperScan(basePackages = HosDataSourceConfig.PACKAGE, sqlSessionFactoryRef = "HosSqlSessionFactory")
public class HosDataSourceConfig {
    static final String PACKAGE = "cn.com.spotty.hos.mapper";

    @Bean(name = "HosDataSource")
    @Primary
    public DataSource hosDataSource() throws IOException {
        // 1. 获取datasource相关信息
        ResourceLoader loader = new DefaultResourceLoader();
        InputStream inputStream = loader.getResource("classpath:application.properties").getInputStream();
        Properties properties = new Properties();
        properties.load(inputStream);
        Set<Object> keys = properties.keySet();

        Properties dsProperties = new Properties();
        for (Object key : keys) {
            if (key.toString().startsWith("datasource")) {
                dsProperties.put(key.toString().replace("datasource.", ""), properties.get(key));
            }
        }
        // 2. 通过hikariDataSourceFactory创建datasource
        HikariDataSourceFactory factory = new HikariDataSourceFactory();
        factory.setProperties(dsProperties);
        inputStream.close();
        return factory.getDataSource();
    }


    @Bean(name = "HosSqlSessionFactory")
    @Primary
    public SqlSessionFactory hosSqlSessionFactory(
            @Qualifier("HosDataSource") DataSource hosDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(hosDataSource);

        // 1. 读取mybatis相关的配置
        ResourceLoader loader = new DefaultResourceLoader();
        sqlSessionFactoryBean.setConfigLocation(loader.getResource("classpath:mybatis-config.xml"));

        // 2. 获取sqlSessionFactory实例
        sqlSessionFactoryBean.setSqlSessionFactoryBuilder(new SqlSessionFactoryBuilder());
        return sqlSessionFactoryBean.getObject();
    }
}
