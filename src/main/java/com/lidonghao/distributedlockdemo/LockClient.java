package com.lidonghao.distributedlockdemo;

import com.lidonghao.distributedlockdemo.exception.LockException;

/**
 * 锁客户端
 */
public interface LockClient {

    /**
     * @方法名称 build
     * @功能描述 获取锁客户端
     * @return 锁客户端对象
     * @throws Exception 创建异常
     */
    LockClient build() throws Exception;

    /**
     *
     * @方法名称 newLock
     * @功能描述 获取锁
     * @param lockKey 锁名称，全局唯一标识
     * @return true-获取锁，false-未获得锁
     * @throws LockException 锁异常
     */
    Lock newLock(String lockKey) throws LockException;

    /**
     * @方法名称 newLock
     * @功能描述 获取锁
     * @param lockKey 锁名称，全局唯一标识
     * @param reentrant 是否支持重入
     * @return true-获取锁，false-未获得锁
     * @throws LockException 锁异常
     */
    Lock newLock(String lockKey, boolean reentrant) throws LockException;

    /**
     * @方法名称 close
     * @功能描述 释放锁
     * @throws LockException 锁异常
     */
    void close() throws LockException;
}
