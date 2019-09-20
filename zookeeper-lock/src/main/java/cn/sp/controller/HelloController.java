package cn.sp.controller;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by 2YSP on 2019/1/27.
 */
@RestController
@RequestMapping("/zoo")
public class HelloController {

    public final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CuratorFramework client;

    @RequestMapping("/hello")
    public String hello(){
        final InterProcessMutex lock = new InterProcessMutex(client, "/lock");
        try {
            lock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
        log.info("{} 获取锁成功",Thread.currentThread().getName());
        System.out.println("执行业务逻辑。。。。");
        try {
            lock.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("{} 释放锁成功",Thread.currentThread().getName());
        return "OK";
    }
}
