package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.Lock;
import com.lidonghao.distributedlockdemo.LockClient;
import com.lidonghao.distributedlockdemo.client.ZookeeperClient;
import com.lidonghao.distributedlockdemo.exception.LockException;

/**
 * @类名称 ZookeeperSolution.java
 * @类描述 Zookeeper 解决方案
 * @作者  ldh
 */
public class ZookeeperSolution implements LockClient {
    /**
     * Zookeeper客户端，
     */
    private ZookeeperClient client;
    /**
     * Zookeeper节点列表
     */
    private String baseUrl;


    public ZookeeperSolution connectionUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }
    @Override
    public LockClient build() throws LockException {
        client = ZookeeperClient.createInstance(baseUrl);
        return this;
    }

    @Override
    public Lock newLock(String lockKey)
            throws LockException {
        return newLock(lockKey, false);
    }

    @Override
    public Lock newLock(String lockKey, boolean reentrant)
            throws LockException {

        return new ZookeeperLock(client, lockKey, reentrant);
    }

    @Override
    public void close()
            throws LockException {
    }

}
