package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.Lock;
import com.lidonghao.distributedlockdemo.LockClient;
import com.lidonghao.distributedlockdemo.client.EtcdClient;
import com.lidonghao.distributedlockdemo.exception.LockException;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * etcd 锁方案
 */
public class EtcdSolution implements LockClient {

    private EtcdClient etcdclient;

    /**
     * etcd节点列表
     */
    private String[] baseUrl;

    /**
     * 集群名
     */
    private String clusterName;

    private int timeout;

    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        }
    });

    public EtcdSolution connectionString(String... baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public EtcdSolution clusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    /**
     * 单位ms，设置后调用acquire方法最大等待时间
     *
     * @param timeout
     * @return
     */
    public EtcdSolution timeout(int timeout) {
        if (timeout < 5 || timeout > TimeUnit.SECONDS.toMillis(2)) {
            throw new IllegalArgumentException("timeout 取值区间【5," + TimeUnit.SECONDS.toMillis(2) + "】");
        }
        this.timeout = timeout;
        return this;
    }

    @Override
    public LockClient build() throws LockException {
        if (baseUrl != null && baseUrl.length > 0) {
            timeout = timeout == 0 ? 1000 : timeout;
            this.etcdclient = EtcdClient.getInstance(baseUrl);
        }
        if (!StringUtils.hasText(clusterName)) {
            throw new LockException("clusterName is must param , please add clusterName param");
        }
        return this;
    }

    @Override
    public Lock newLock(String lockKey)
            throws LockException {
        return newLock(lockKey, false);
    }

    @Override
    public Lock newLock(String lockKey, boolean reentrant) throws LockException {
        return new EtcdLock(etcdclient, clusterName, lockKey, reentrant);
    }

    @Override
    public synchronized void close() throws LockException {

    }
}
