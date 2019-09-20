package cn.sp.service;

import cn.sp.lock.DistributedLock;
import org.springframework.stereotype.Service;

/**
 * Created by 2YSP on 2019/9/20.
 */
@Service
public class TestService {

  @DistributedLock(retryTimes = 1000,timeout = 1000)
  public String lockTest() {
    try {
      System.out.println("模拟执行业务逻辑。。。");
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return "error";
    }

    return "ok";
  }
}
