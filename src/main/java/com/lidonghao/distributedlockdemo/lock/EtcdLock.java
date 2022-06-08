package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.client.EtcdClient;
import com.lidonghao.distributedlockdemo.client.EtcdResponse;
import com.lidonghao.distributedlockdemo.exception.LockException;
import com.lidonghao.distributedlockdemo.util.RandomUtil;
import com.lidonghao.distributedlockdemo.util.RenewTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;

/**
 * etcd锁对象
 */
public class EtcdLock extends BaseLock {
    private static Logger logger = LoggerFactory.getLogger("etcd-lock");

    protected EtcdClient client;

    /**
     * 续租线程
     */
    private RenewTask hbThread = null;

    EtcdLock(EtcdClient client, String clusterName, String key, boolean reentrant) {
        super(clusterName + "_" + key, "EtcdLock_Name_" + RandomUtil.nextInt(), reentrant);
        this.client = client;
    }

    @Override
    protected boolean lock() throws LockException {
        try {
            // 抢锁
            EtcdResponse etcdResult = client.casVal(key, value, this.ttl);
            if ((etcdResult != null && !etcdResult.isError()) && (etcdResult.httpCode == 200 || etcdResult.httpCode == 201)) {
                hbThread = new RenewTask(() -> {
                    EtcdResponse result = client.casExist(key, value, ttl);
                    if (result.isError()) {
                        close();
                    }
                }, this.ttl);
                hbThread.setDaemon(true);
                hbThread.start();
                hold.set(true);
                logger.debug(key + "抢锁成功!");
            } else {
                hold.set(false);
                if (etcdResult != null && etcdResult.isError() && (etcdResult.errorCode > 200)) {
                    throw new LockException("etcd cluster Related Error or param error : code" + etcdResult.errorCode, etcdResult);
                }
            }
        } catch (LockException e) {
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
    public void release() throws LockException {
        if (hold.get()) {
            try {
                hold.set(false);
                holdLocks.get().remove(key);
                client.casDelete(key, value);
                logger.debug(key + "放锁成功!");
            } catch (LockException e) {
                logger.warn(e.getMessage());
            } finally {
                if (hbThread != null) {
                    hbThread.close();
                }
            }
        }
    }

    @Override
    protected Object getClient() {
        return client;
    }

    public String getLockValue() {
        return value;
    }
}
