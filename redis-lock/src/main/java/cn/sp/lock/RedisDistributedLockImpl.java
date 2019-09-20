package cn.sp.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

/**
 * Redis 分布式锁实现类
 * Created by 2YSP on 2019/9/20.
 */
@Slf4j
@Component
public class RedisDistributedLockImpl implements IRedisDistributedLock {

  /**
   * key前缀
   */
  public static final String PREFIX = "Lock_";
  /**
   * 保存锁的value
   */
  private ThreadLocal<String> threadLocal = new ThreadLocal<>();

  @Autowired
  private RedisTemplate redisTemplate;

  private DefaultRedisScript<Long> lockRedisScript;
  private DefaultRedisScript<Long> releaseRedisScript;

  @PostConstruct
  public void init() {
    lockRedisScript = new DefaultRedisScript<>();
    lockRedisScript.setResultType(Long.class);
    lockRedisScript
        .setScriptSource(new ResourceScriptSource(new ClassPathResource("/requireLock.lua")));

    releaseRedisScript = new DefaultRedisScript<>();
    releaseRedisScript.setResultType(Long.class);
    releaseRedisScript
        .setScriptSource(new ResourceScriptSource(new ClassPathResource("/releaseLock.lua")));
  }

  @Override
  public boolean lock(String key, long requireTimeOut, long lockTimeOut, int retries) {
    //可重入锁判断
    String originValue = threadLocal.get();
    if (!StringUtils.isBlank(originValue) && isReentrantLock(key, originValue)) {
      return true;
    }
    String value = UUID.randomUUID().toString();
    long end = System.currentTimeMillis() + requireTimeOut;
    int expire = (int) (lockTimeOut / 1000);
    int retryTimes = 1;

    try {
      while (System.currentTimeMillis() < end) {
        if (retryTimes > retries) {
          log.error(" require lock failed,retry times [{}]", retries);
          return false;
        }
        if (setNX(wrapLockKey(key), value, expire)) {
          threadLocal.set(value);
          return true;
        }
        // 休眠10ms
        Thread.sleep(10);

        retryTimes++;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  private boolean setNX(String key, String value, int expire) {
    /**
     * List设置lua的keys
     */
    List<String> keyList = new ArrayList<>();
    keyList.add(key);
    //返回结果为NULL
    Long result = (Long) redisTemplate.execute(lockRedisScript, keyList, expire, value);

    if (result == null || result != 0) {
      return true;
    }
    return false;
  }

  /**
   * 是否为重入锁
   */
  private boolean isReentrantLock(String key, String originValue) {
    String v = (String) redisTemplate.opsForValue().get(key);
    return v != null && originValue.equals(v);
  }

  @Override
  public boolean release(String key) {
    String originValue = threadLocal.get();
    if (StringUtils.isBlank(originValue)) {
      return false;
    }
    List<String> keyList = new ArrayList<>();
    keyList.add(wrapLockKey(key));
    Long result = (Long) redisTemplate.execute(releaseRedisScript, keyList, originValue);
    return result > 0;
  }


  private String wrapLockKey(String key) {
    return PREFIX + key;
  }
}
