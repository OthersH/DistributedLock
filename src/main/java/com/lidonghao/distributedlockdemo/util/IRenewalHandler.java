package com.lidonghao.distributedlockdemo.util;

import com.lidonghao.distributedlockdemo.exception.LockException;

/**
 * 续租Handler
 */
public interface IRenewalHandler {

    /**
     * 实现续租方法
     * @throws LockException
     */
    public void callBack() throws LockException;
}
