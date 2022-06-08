package com.lidonghao.distributedlockdemo.exception;

import com.lidonghao.distributedlockdemo.client.EtcdResponse;

/**
 * 锁异常类
 */
public class LockException extends Exception{


    final Integer httpCode;

    final EtcdResponse result;

    public EtcdResponse getResult() {
        return result;
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
        this.httpCode = null;
        this.result = null;
    }

    public LockException(String message) {
        super(message);
        this.httpCode = null;
        this.result = null;
    }

    public LockException(String message, int httpCode) {
        super(message + "(" + httpCode + ")");
        this.httpCode = httpCode;
        this.result = null;
    }

    public LockException(String message, EtcdResponse result) {
        super(message);
        this.httpCode = null;
        this.result = result;
    }

    public int gethttpCode() {
        return httpCode;
    }

    public boolean isHttpError(int httpCode) {
        return (this.httpCode != null && httpCode == this.httpCode);
    }

    public boolean isEtcdError(int etcdCode) {
        return (this.result != null && this.result.errorCode != null && etcdCode == this.result.errorCode);
    }

    public boolean isNetError() {
        return result == null;
    }
}
