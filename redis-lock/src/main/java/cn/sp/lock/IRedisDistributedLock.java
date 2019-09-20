package cn.sp.lock;

/**
 * Redis分布式锁接口
 * Created by 2YSP on 2019/9/20.
 */
public interface IRedisDistributedLock {

  /**
   *
   * @param key
   * @param requireTimeOut 获取锁超时时间 单位ms
   * @param lockTimeOut 锁过期时间，一定要大于业务执行时间 单位ms
   * @param retries 尝试获取锁的最大次数
   * @return
   */
  boolean lock(String key, long requireTimeOut, long lockTimeOut, int retries);

  /**
   * 释放锁
   * @param key
   * @return
   */
  boolean release(String key);

}
