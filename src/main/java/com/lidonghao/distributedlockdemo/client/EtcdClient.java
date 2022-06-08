package com.lidonghao.distributedlockdemo.client;

import com.alibaba.fastjson.JSON;
import com.lidonghao.distributedlockdemo.exception.LockException;
import com.lidonghao.distributedlockdemo.util.CurlUtil;
import com.lidonghao.distributedlockdemo.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * etcd客户端对象：支持etcd集群节点设置，并支持etcd节点自动探活
 */
public class EtcdClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdClient.class);

    /**
     * 最大重试次数
     */
    private static final int CONNECT_RETRY_COUNT_MAX = 2;

    /**
     * 所有etcd节点
     */
    private final List<String> allEtcdNodes = new ArrayList<String>();

    /**
     * 活跃节点列表
     */
    private List<String> availableEtcdNodes = new ArrayList<String>();

    /**
     * 失联节点列表
     */
    private List<String> brokenEtcdNodes = new ArrayList<String>();

    /**
     * etcd 状态更改重入锁
     */
    private ReentrantLock changeEtcdNodeStatusLock = new ReentrantLock();

    /**
     * etcd节点心跳任务
     */
    private final EtcdHeartbeatTask etcdHeartbeatTask;

    /**
     * @param baseUrl etcd节点列表
     */
    private EtcdClient(String... baseUrl) {
        if (baseUrl == null || baseUrl.length == 0) {
            throw new IllegalArgumentException("Lock EtcdClient URL can not be empty ...");
        }
        for (int i = 0; i < baseUrl.length; i++) {
            if (!baseUrl[i].endsWith("/")) {
                baseUrl[i] += "/";
            }
            allEtcdNodes.add(baseUrl[i]);
        }
        availableEtcdNodes.addAll(allEtcdNodes);
        etcdHeartbeatTask = new EtcdHeartbeatTask(this);
        etcdHeartbeatTask.setDaemon(true);
        etcdHeartbeatTask.setName("EtcdHeartbeatTask");
        etcdHeartbeatTask.start();
    }

    public static EtcdClient getInstance(String... baseUrl) {
        return new EtcdClient(baseUrl);
    }

    /**
     * @方法名称 casVal
     * @功能描述 etcd 原子赋值操作
     * @param key 锁名
     * @param value 锁值
     * @param ttl 锁过期时间
     * @return 操作结果
     * @throws LockException 锁异常
     */
    public EtcdResponse casVal(String key, String value, Integer ttl) throws LockException {
        try {

            return syncput(key, value, null, false, ttl, 0);
        } catch (Exception e) {

            Throwable cause = e.getCause();
            if (cause instanceof LockException) {
                throw (LockException)cause;
            }
            throw new LockException("Error executing request", e);
        }
    }

    /**
     * @方法名称 casExist
     * @功能描述 etcd 原子刷新操作
     * @param key 锁名
     * @param value 锁值
     * @param exist 是否存在：true/false
     * @param ttl 锁过期时间
     * @return 操作结果
     * @throws LockException 锁异常
     */
    public EtcdResponse casExist(String key, String value, Integer ttl) throws LockException {
        try {
            return syncput(key, null, value, true, ttl, 0);
        } catch (LockException e) {
            throw e;
        }
    }

    /**
     * @方法名称 casExist
     * @功能描述 etcd 原子删除操作
     * @param key 锁名
     * @param prevValue 前任锁值
     * @return 操作结果
     * @throws LockException 锁异常
     */
    public EtcdResponse casDelete(String key, String prevValue)
            throws LockException {
        try {
            return syncdelete(key, prevValue, 0);
        } catch (LockException e) {
            throw e;
        }
    }

    /**
     * @方法名称 syncput
     * @功能描述 同步put请求
     * @param key 锁名
     * @param value 锁值
     * @param prevValue 前任锁值
     * @param exist 是否存在
     * @param ttl 锁时间/过期时间
     * @param connectRetryCount 重试次数，最大值为2
     * @return etcd响应结果
     * @throws LockException 锁异常
     */
    private EtcdResponse syncput(String key, String value, String prevValue, Boolean exist, Integer ttl, int connectRetryCount)
            throws LockException {
        String url = StringUtils.hasText(prevValue) ? buildURI("v2/keys", key).concat("?prevValue=").concat(prevValue) : buildURI("v2/keys", key);
        List<String> commonds = new ArrayList<String>();
        commonds.add(this.getRandomAvailableEtcdNode() + url);
        commonds.add("-XPUT");
        if (StringUtils.hasText(value)) {
            commonds.add("-d");
            commonds.add("value=".concat(value));
        }
        if (null != exist) {
            commonds.add("-d");
            commonds.add("prevExist=".concat(exist ? "true" : "false"));
            if (exist) {
                commonds.add("-d");
                commonds.add("refresh=true");
            }
        }
        if (ttl != null) {
            commonds.add("-d");
            commonds.add("ttl=".concat(ttl.toString()));
        }
        try {
            String json = CurlUtil.execCurl(commonds);
            return StringUtils.hasText(json) ? JSON.parseObject(json, EtcdResponse.class) : null;
        } catch (RuntimeException e) {
            if (connectRetryCount > CONNECT_RETRY_COUNT_MAX) {
                throw new LockException("Lock etcd node cannot connect error", e);
            }
            connectRetryCount++;
            return syncput(key, value, prevValue, exist, ttl, connectRetryCount);
        } catch (Exception e1) {
            throw new LockException("sync http request : [error sync http request exception]", e1);
        }
    }

    /**
     * @方法名称 syncdelete
     * @功能描述 同步delete请求
     * @param key 锁名
     * @param prevValue 前任锁值
     * @param connectRetryCount 重试次数，最大值为2
     * @return etcd响应结果
     * @throws LockException 锁异常
     */
    private EtcdResponse syncdelete(String key, String prevValue, int connectRetryCount)
            throws LockException {
        String url = buildURI("v2/keys", key).concat("?prevValue=").concat(prevValue);
        try {
            List<String> commonds = new ArrayList<String>();
            commonds.add(this.getRandomAvailableEtcdNode() + url);
            commonds.add("-XDELETE");
            String json = CurlUtil.execCurl(commonds);
            return StringUtils.hasText(json) ? JSON.parseObject(json, EtcdResponse.class) : null;
        } catch (RuntimeException e) {
            if (connectRetryCount > CONNECT_RETRY_COUNT_MAX) {
                LOGGER.warn("------Lock etcd node cannot connect error-----" + e.getMessage());
                throw new LockException("Lock etcd node cannot connect error", e);
            }
            connectRetryCount++;
            return syncdelete(key, prevValue, connectRetryCount);
        } catch (Exception e1) {
            LOGGER.error("------sync http request exception error-----", e1);
            throw new LockException("sync http request : [error sync http request exception]", e1);
        }
    }

    /**
     * @方法名称 buildURI
     * @功能描述 生成请求地址：对锁名进行url编码防止地址错误
     * @param prefix 前缀
     * @param key 锁名
     * @return 请求地址
     * @throws LockException 锁名异常
     */
    private String buildURI(String prefix, String key)
            throws LockException {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        String[] keys = StringUtils.split(key, "/");
        if (null == keys) {
            keys = new String[] {key};
        }
        for (String subKey : keys) {
            sb.append("/");
            try {
                sb.append(URLEncoder.encode(subKey, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new LockException("Lock key Non-compliant: ".concat(subKey));
            }
        }
        return URI.create(sb.toString()).getPath();
    }

    /**
     * @方法名称 setAvailableEtcdNode
     * @功能描述 追加活跃节点
     * @param url 节点地址
     */
    void setAvailableEtcdNode(String url) {
        try {
            if (changeEtcdNodeStatusLock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    List<String> availableEtcdNodes = new ArrayList<String>(this.availableEtcdNodes);
                    List<String> brokenEtcdNodes = new ArrayList<String>(this.brokenEtcdNodes);

                    availableEtcdNodes.add(url);
                    brokenEtcdNodes.remove(url);

                    this.availableEtcdNodes = availableEtcdNodes;
                    this.brokenEtcdNodes = brokenEtcdNodes;
                } finally {
                    changeEtcdNodeStatusLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @方法名称 setBrokenEtcdNode
     * @功能描述 追加失联节点
     * @param url 节点地址
     */
    void setBrokenEtcdNode(String url) {
        try {
            if (changeEtcdNodeStatusLock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    List<String> availableEtcdNodes = new ArrayList<String>(this.availableEtcdNodes);
                    List<String> brokenEtcdNodes = new ArrayList<String>(this.brokenEtcdNodes);

                    availableEtcdNodes.remove(url);
                    brokenEtcdNodes.add(url);

                    this.availableEtcdNodes = availableEtcdNodes;
                    this.brokenEtcdNodes = brokenEtcdNodes;
                } finally {
                    changeEtcdNodeStatusLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @方法名称 getRandomAvailableEtcdNode
     * @功能描述 获取etcd随机活跃节点
     * @return etcd节点
     */
    private String getRandomAvailableEtcdNode() {
        List<String> availableEtcdNodes = this.availableEtcdNodes;
        if (availableEtcdNodes.size() == 0) {
            LOGGER.error("Lock all etcd nodes has broken, use  var [allEtcdNodes] instead...");
            availableEtcdNodes = allEtcdNodes;
        }
        return availableEtcdNodes.get(RandomUtil.nextInt(0, availableEtcdNodes.size() - 1));
    }

    List<String> getAllEtcdNodes() {
        return this.allEtcdNodes;
    }

    List<String> getAvailableEtcdNodes() {
        return this.availableEtcdNodes;
    }

    List<String> getBrokenEtcdNodes() {
        return this.brokenEtcdNodes;
    }
}
