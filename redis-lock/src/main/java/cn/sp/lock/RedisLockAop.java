package cn.sp.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by 2YSP on 2019/9/20.
 */
@Component
@Aspect
@Slf4j
public class RedisLockAop {

  @Autowired
  private IRedisDistributedLock redisDistributedLock;


  @Around(value = "@annotation(lock)")
  public Object doAroundAdvice(ProceedingJoinPoint proceedingJoinPoint, DistributedLock lock) {
    // 加锁
    String key = getKey(proceedingJoinPoint, lock);
    Boolean success = null;
    try {
        success = redisDistributedLock
          .lock(key, lock.timeout(), lock.expire(), lock.retryTimes());
      if (success) {
        log.info(Thread.currentThread().getName() + " 加锁成功");
        return proceedingJoinPoint.proceed();
      }
      log.info(Thread.currentThread().getName() + " 加锁失败");
      return null;
    } catch (Throwable throwable) {
      throwable.printStackTrace();
      return null;
    } finally {
      if (success){
        boolean result = redisDistributedLock.release(key);
        log.info(Thread.currentThread().getName() + " 释放锁结果:{}",result);
      }
    }
  }

  private String getKey(JoinPoint joinPoint, DistributedLock lock) {
    if (!StringUtils.isBlank(lock.key())) {
      return lock.key();
    }
    return joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature()
        .getName();
  }
}
