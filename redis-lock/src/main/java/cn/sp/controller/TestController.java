package cn.sp.controller;

import cn.sp.lock.RedisLock;
import cn.sp.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by 2YSP on 2019/1/26.
 */
@RestController
public class TestController {

    public final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TestService testService;

    @Autowired
    RedisLock redisLock;

    @RequestMapping("/test")
    public String  test(){
        String lockName = "lock:test";
        //获取锁
//        String result = redisLock.requireLock(lockName, 2000);
//        String result = redisLock.requireLockWithTimeOut(lockName,2000,10000);
        String result = redisLock.requireLockWithLua(lockName,2000,10000);
        if (StringUtils.isBlank(result)){
            log.info("线程:{} 获取锁[{}]失败",Thread.currentThread().getName(),lockName);
            return "error";
        }else {
            log.info("线程{} 获取锁[{}]成功 id:{}",Thread.currentThread().getName(),lockName,result);
            System.out.println("执行业务逻辑。。。。");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //释放锁
//            boolean success = redisLock.releaseLock(lockName, result);
            //使用Lua脚本释放锁
            boolean success = redisLock.releaseLockWithLua(lockName,result);
            log.info("释放锁[{}] {}",lockName,success ? "成功": "失败");
        }
        return "ok";
    }


    @GetMapping("/lock/test")
    public String lockTest(){
        return testService.lockTest();
    }

}
