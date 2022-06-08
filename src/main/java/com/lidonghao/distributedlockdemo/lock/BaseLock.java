package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.Lock;
import com.lidonghao.distributedlockdemo.exception.LockException;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BaseLock extends Lock {
    /**
     * 默认过期时间，单位s
     */
    protected final static int DEFAULT_SENCOND = 10;

    /**
     * 活跃重入锁
     */
    protected static final ThreadLocal<HashSet<String>> holdLocks = new ThreadLocal<HashSet<String>>() {
        @Override
        protected HashSet<String> initialValue() {
            return new HashSet<String>();
        }
    };

    /**
     * 锁名
     */
    protected String key;

    /**
     * 锁值
     */
    protected String value;

    /**
     * 过期时间，单位s
     */
    protected Integer ttl;

    /**
     * 是否支持锁重入
     */
    protected boolean reentrant;

    /**
     * 是否已经持有锁
     */
    protected final AtomicBoolean hold = new AtomicBoolean(false);

    public BaseLock(String key, String value, boolean reentrant) {
        this.key = key;
        this.value = value;
        this.ttl = DEFAULT_SENCOND;
        this.reentrant = reentrant;
    }


    @Override
    public boolean acquire(int ttl) throws LockException {
        if (getClient() == null || !StringUtils.hasLength(key)) {
            // 空锁
            return false;
        } else if (reentrant && holdLocks.get().contains(key)) {
            // 重入锁
            return true;
        }
        this.ttl = (ttl <= 0 ? DEFAULT_SENCOND : ttl);
        boolean isAcquire = lock();
        return isAcquire;
    }

    @Override
    public boolean acquire(int ttl, long interval, int maxRetry)
            throws LockException {
        if (getClient() == null || !StringUtils.hasLength(key)) {
            // 空锁
            return false;
        } else if (reentrant && holdLocks.get().contains(key)) {
            // 重入锁
            return true;
        }
        this.ttl = (ttl <= 0 ? DEFAULT_SENCOND : ttl);
        try {
            if (!lock()) {
                // 重试抢锁
                if (maxRetry > 0) {
                    Thread.sleep((interval * 1000) <= 0 ? 1 : (interval * 1000));
                    return acquire(ttl, interval, maxRetry - 1);
                }
            }
        } catch (Exception e) {
            if (maxRetry > 0) {
                try {
                    Thread.sleep((interval * 1000) <= 0 ? 1 : (interval * 1000));
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                return acquire(ttl, interval, maxRetry - 1);
            }
        }
        return hold.get();
    }

    @Override
    public void close() throws LockException {
        try {
            if (hold.get()) {
                release();
            }
        } catch (LockException ex) {
            throw ex;
        }
    }

    /**
     * @方法名称 lock
     * @功能描述 抢锁
     * @return true-获取锁，false-未获得锁
     * @throws LockException 锁异常
     */
    protected abstract boolean lock() throws LockException;

    /**
     * @方法名称 release
     * @功能描述 <pre>释放锁</pre>
     * @throws LockException 锁异常
     */
    @Override
    public abstract void release() throws LockException;

    /**
     * @方法名称 getClient
     * @功能描述 <pre>获取方案客户端</pre>
     */
    protected abstract Object getClient();
}
