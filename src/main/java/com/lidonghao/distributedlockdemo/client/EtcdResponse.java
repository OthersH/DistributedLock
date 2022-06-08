package com.lidonghao.distributedlockdemo.client;

import com.alibaba.fastjson.JSON;

/**
 * 节点响应对象
 */
public class EtcdResponse {

    /**
     * http状态码
     */
    public Integer httpCode = 200;

    /**
     * 服务码
     */
    public Integer serverCode;

    public String action;

    /**
     * 节点数据
     */
    public EtcdNodeData node;

    /**
     * 前任节点数据
     */
    public EtcdNodeData prevNode;

    /**
     * 错误码
     */
    public Integer errorCode;

    /**
     * 消息
     */
    public String message;

    /**
     * 异常原因
     */
    public String cause;

    /**
     * 错误索引
     */
    public int errorIndex;

    public boolean isError() {
        return errorCode != null;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

    public String getAction() {
        return action;
    }

    public EtcdNodeData getNode() {
        return node;
    }

    public EtcdNodeData getPrevNode() {
        return prevNode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getCause() {
        return cause;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public Integer getHttpCode() {
        return httpCode;
    }

    public EtcdResponse setHttpCode(Integer httpCode) {
        this.httpCode = httpCode;
        return this;
    }

    public static EtcdResponse create() {
        return new EtcdResponse();
    }
}
