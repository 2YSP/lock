package cn.sp.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by 2YSP on 2019/9/20.
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributedLock {

  /**
   * 默认包名加方法名
   * @return
   */
  String key() default "";

  /**
   * 过期时间 单位：毫秒
   * <pre>
   *     过期时间一定是要长于业务的执行时间.
   * </pre>
   */
  long expire() default 30000;

  /**
   * 获取锁超时时间 单位：毫秒
   * <pre>
   *     结合业务,建议该时间不宜设置过长,特别在并发高的情况下.
   * </pre>
   */
  long timeout() default 3000;

  /**
   * 默认重试次数
   * @return
   */
  int retryTimes() default Integer.MAX_VALUE;

}
