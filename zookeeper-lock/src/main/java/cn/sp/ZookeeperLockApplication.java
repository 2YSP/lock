package cn.sp;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZookeeperLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZookeeperLockApplication.class, args);
	}

	@Bean
	public CuratorFramework client(){
		CuratorFramework client = CuratorFrameworkFactory.builder()
				.connectString("198.13.40.234:2181,198.13.40.234:2182,198.13.40.234:2183")
				.retryPolicy(new ExponentialBackoffRetry(1000, 3)).build();
		client.start();
		return client;
	}

}

