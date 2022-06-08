package com.lidonghao.distributedlockdemo.lock;

import com.lidonghao.distributedlockdemo.client.ZookeeperClient;
import com.lidonghao.distributedlockdemo.exception.LockException;
import com.lidonghao.distributedlockdemo.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.TreeSet;

/**
 * @类名称 ZookeeperLock.java
 * @类描述 Zookeeper 锁
 *
 */
public class ZookeeperLock extends BaseLock {
    private static Logger logger = LoggerFactory.getLogger("zookeeper-lock");

    private final static String RESULT_OK = "OK";

    /**
     * Zookeeper客户端
     */
    ZookeeperClient client;

    /**
     * 当前线程创建的序列node
     */
    private ThreadLocal<String> nodeId = new ThreadLocal<>();

    public ZookeeperLock(ZookeeperClient client, String key, boolean reentrant) {
        super(key, RandomUtil.uuid(), reentrant);
        this.client = client;
    }

    @Override
    protected boolean lock() throws LockException {
        try {
            String myNode = client.createTempNode(key, value);
            TreeSet<String> sortedNodes = client.getAllLockNodes();
            String smallNode = sortedNodes.first();

            String lowerNode = sortedNodes.lower(myNode);
            if (myNode.equals(smallNode)) {
                //如果是最小节点 则表示获得锁
                nodeId.set(myNode);
                hold.set(true);
            } else {
                hold.set(false);
            }
        } catch (Exception e) {
            hold.set(false);
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) {
                return hold.get();
            }
            logger.error("Error encountered when attempting to acquire lock", e);
            throw new LockException(e.getMessage());
        } finally {
            if (reentrant && hold.get()) {
                holdLocks.get().add(key);
            }
        }
        return hold.get();
    }

    @Override
    public void release()
            throws LockException {
        if (hold.get()) {
            try {
                hold.set(false);
                if (null != nodeId) {
                    client.deleteNode(nodeId.get());
                }
                nodeId.remove();
                holdLocks.get().remove(key);
            } catch (Exception e) {
                logger.error("Error encountered when attempting to release lock", e);
                throw new LockException(e.getMessage());
            }
        }
    }

    @Override
    protected Object getClient() {
        return client;
    }
}
