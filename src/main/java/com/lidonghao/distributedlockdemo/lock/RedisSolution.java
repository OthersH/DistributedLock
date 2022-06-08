package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.Lock;
import com.lidonghao.distributedlockdemo.LockClient;
import com.lidonghao.distributedlockdemo.client.RedisClient;
import com.lidonghao.distributedlockdemo.exception.LockException;

/**
 *  redis 解决方案
 */
public class RedisSolution implements LockClient {

    /**
     * redis客户端、
     */
    private RedisClient client;

    @Override
    public LockClient build() throws Exception {
        return this;
    }

    @Override
    public Lock newLock(String lockKey) throws LockException {
        return newLock(lockKey,false);
    }

    @Override
    public Lock newLock(String lockKey, boolean reentrant) throws LockException {
        client = RedisClient.createInstance();
        return new RedisLock(client,lockKey,reentrant);
    }

    @Override
    public void close() throws LockException {

    }
}
