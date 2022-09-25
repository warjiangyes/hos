package cn.com.spotty.hos;

import cn.com.spotty.hos.core.HosConfiguration;
import cn.com.spotty.hos.server.HdfsServiceImpl;
import cn.com.spotty.hos.server.HosStoreImpl;
import cn.com.spotty.hos.server.IHosStore;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HosServerBeanConfiguration {

    @Bean
    public Connection getConnection() throws IOException {
        // 获取hbase connection并注入到bean中
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        HosConfiguration confUtil = HosConfiguration.getConfiguration();

        config.set("hbase.zookeeper.quorum", confUtil.getString("hbase.zookeeper.quorum"));
        config.set("hbase.zookeeper.property.clientPort", confUtil.getString("hbase.zookeeper.port"));
        config.set(HConstants.HBASE_RPC_TIMEOUT_KEY, "3600000");

        return ConnectionFactory.createConnection(config);
    }

    @Bean(name = "hosStoreService")
    public IHosStore getHosStore(@Autowired Connection connection) throws Exception {
        // 实例化一个HosStore实例
        HosConfiguration confUtil = HosConfiguration.getConfiguration();
        String zkHosts = confUtil.getString("hbase.zookeeper.quorum");
        return new HosStoreImpl(connection, new HdfsServiceImpl(), zkHosts);
    }
}
