package com.lidonghao.distributedlockdemo.util;

import com.lidonghao.distributedlockdemo.exception.LockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * 心跳续租线程
 */
public class RenewTask extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenewTask.class);
    public volatile boolean isRunning = true;
    /**
     * 过期时间，单位s
     */
    private int ttl;

    private IRenewalHandler call;

    public RenewTask(IRenewalHandler call, int ttl) {
        this.ttl = ttl <= 0 ? 10 : ttl;
        this.call = call;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                // 1、续租，刷新值
                call.callBack();
                LOGGER.debug("续租成功!");
                // 2、三分之一过期时间续租
                TimeUnit.SECONDS.sleep(this.ttl * 1000 / 3);
            } catch (InterruptedException e) {
                close();
            } catch (LockException e) {
                close();
            }
        }
    }

    public void close() {
        isRunning = false;
    }
}
