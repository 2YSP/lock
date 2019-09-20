package cn.sp.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.lang.Nullable;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by 2YSP on 2019/1/26.
 */
@Component
public class RedisLock {

    //    @Autowired
//    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    private DefaultRedisScript<Long> redisScript;
    private DefaultRedisScript<Long> lockRedisScript;

    @PostConstruct
    public void init() {
        redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/releaseLock.lua")));

        lockRedisScript = new DefaultRedisScript<>();
        lockRedisScript.setResultType(Long.class);
        lockRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("/requireLock.lua")));
    }

    /**
     * 获取锁
     *
     * @param lockName
     * @param requireTimeOut
     * @return
     */
    public String requireLock(String lockName, long requireTimeOut) {
        String key = lockName;
        String identifier = UUID.randomUUID().toString();
        long end = System.currentTimeMillis() + requireTimeOut;
        while (System.currentTimeMillis() < end) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, identifier);
            if (success) {
                return identifier;
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param lockName
     * @param identifier
     * @return
     */
    public boolean releaseLock(String lockName, String identifier) {
        String key = lockName;
        while (true) {
            redisTemplate.watch(key);
            if (identifier.equals(redisTemplate.opsForValue().get(key))) {
                //检查是否还未释放
                SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
                    @Nullable
                    @Override
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        operations.multi();
                        operations.delete(key);
                        List obj = operations.exec();
                        return obj;
                    }
                };
                Object object = redisTemplate.execute(sessionCallback);
                if (object != null) {
                    return true;
                }
                continue;
            }
            redisTemplate.unwatch();
            break;
        }
        return false;
    }

    /**
     * @param lockName
     * @param requireTimeOut
     * @return
     */
    public String requireLockWithTimeOut(String lockName, long requireTimeOut, long lockTimeOut) {
        String key = lockName;
        String identifier = UUID.randomUUID().toString();
        long end = System.currentTimeMillis() + requireTimeOut;
        int lockExpire = (int) (lockTimeOut / 1000);
        while (System.currentTimeMillis() < end) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(key, identifier);
            if (success) {
                //设置过期时间
                redisTemplate.expire(key, lockExpire, TimeUnit.SECONDS);
                return identifier;
            }

            if (redisTemplate.getExpire(key, TimeUnit.SECONDS) == -1) {
                redisTemplate.expire(key, lockExpire, TimeUnit.SECONDS);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 使用Lua脚本释放锁
     *
     * @param lockName
     * @param identifier
     * @return
     */
    public boolean releaseLockWithLua(String lockName, String identifier) {
        /**
         * List设置lua的keys
         */
        List<String> keyList = new ArrayList<>();
        keyList.add(lockName);
        Long result = (Long) redisTemplate.execute(redisScript, keyList, identifier);
        return result > 0;
    }

    /**
     * 使用Lua脚本获取锁
     *
     * @param lockName
     * @param requireTimeOut 获取锁的超时时间 ms
     * @param lockTimeOut    锁的有效时间 ms
     * @return
     */
    public String requireLockWithLua(String lockName, long requireTimeOut, long lockTimeOut) {
        String identifier = UUID.randomUUID().toString();
        long end = System.currentTimeMillis() + requireTimeOut;
        int lockExpire = (int) (lockTimeOut / 1000);
        while (System.currentTimeMillis() < end) {
            /**
             * List设置lua的keys
             */
            List<String> keyList = new ArrayList<>();
            keyList.add(lockName);
            //返回结果为NULL
            Long result = (Long) redisTemplate.execute(lockRedisScript, keyList, lockExpire , identifier);
            if (result == null || result != 0){
                return identifier;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
