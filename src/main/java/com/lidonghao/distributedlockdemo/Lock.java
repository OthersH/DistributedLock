package com.lidonghao.distributedlockdemo;

import com.lidonghao.distributedlockdemo.exception.LockException;

/**
 * 分布式锁对象
 */
public abstract class Lock implements AutoCloseable{

    static LockFactory lockFactory =new LockFactory();
    /**
     * @方法名称 acquire
     * @功能描述 获取锁，如果没得到，不阻塞
     * @param ttl 过期时间，单位s
     * @return
     * @throws LockException
     */
    public abstract boolean acquire(int ttl) throws LockException;

    /**
     * @方法名称 acquire
     * @功能描述 获取锁，直到超时
     * @param ttl 过期时间，单位s
     * @param interval 重试间隔时间，单位s
     * @param maxRetry 最大重试次数
     * @return
     * @throws LockException 锁异常
     */
    public abstract boolean acquire(int ttl, long interval, int maxRetry) throws LockException;

    /**
     * 释放锁
     */
    public abstract void release()
            throws LockException;
    /**
     * 获取锁工厂
     */
    public static LockFactory factory() {
        return lockFactory;
    }
}
