package com.lidonghao.distributedlockdemo;

import com.lidonghao.distributedlockdemo.lock.EtcdSolution;
import com.lidonghao.distributedlockdemo.lock.RedisSolution;
import com.lidonghao.distributedlockdemo.lock.ZookeeperSolution;

/**
 * 锁工厂
 */
public class LockFactory {
    public LockFactory() {
    }

    /**
     * 获取etcd锁方案对象
     */
    public EtcdSolution etcdSolution() {
        return new EtcdSolution();
    }

    /**
     * 获取redis锁方案对象
     */
    public RedisSolution redisSolution() {
        return new RedisSolution();
    }

    /**
     * 获取Zookeeper锁方案对象
     */
    public ZookeeperSolution zookeeperSolution() {
        return new ZookeeperSolution();
    }
}
