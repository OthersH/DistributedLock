package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.client.RedisClient;
import com.lidonghao.distributedlockdemo.exception.LockException;
import com.lidonghao.distributedlockdemo.util.IRenewalHandler;
import com.lidonghao.distributedlockdemo.util.RandomUtil;
import com.lidonghao.distributedlockdemo.util.RenewTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;

public class RedisLock extends BaseLock {

    private static Logger logger = LoggerFactory.getLogger("redis-lock");

    private final static String RESULT_OK = "OK";

    /**
     * redis客户端，基于jedis 实现
     */
    RedisClient client;

    /**
     * 续租线程
     */
    RenewTask renewalTask;

    public RedisLock(RedisClient client, String key, boolean reentrant) {
        super(key, RandomUtil.uuid(), reentrant);
        this.client = client;
    }

    @Override
    protected boolean lock()
            throws LockException {
        try {
            // 抢锁
            if (RESULT_OK.equals(client.setNxPx(key, value, this.ttl))) {

                // NxMonitor.sum(30, 1);
                renewalTask = new RenewTask(new IRenewalHandler() {
                    @Override
                    public void callBack()
                            throws LockException {
                        // 刷新值
                        client.expire(key, ttl <= 0 ? 10 : ttl);
                    }
                }, ttl);
                renewalTask.setDaemon(true);
                renewalTask.start();
                hold.set(true);
            } else {
                hold.set(false);
            }
        } catch (Exception e) {
            hold.set(false);
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) {
                return hold.get();
            }
            logger.error("Error encountered when attempting to acquire lock", e);
            throw e;
        } finally {
            if (reentrant && hold.get()) {
                holdLocks.get().add(key);
            }
        }
        return hold.get();
    }

    @Override
    public void release()
            throws LockException {
        if (hold.get()) {
            try {
                hold.set(false);
                client.delete(key, value);
                holdLocks.get().remove(key);
            } finally {
                if (renewalTask != null) {
                    renewalTask.close();
                }
            }
        }
    }

    @Override
    protected Object getClient() {
        return client;
    }
}
