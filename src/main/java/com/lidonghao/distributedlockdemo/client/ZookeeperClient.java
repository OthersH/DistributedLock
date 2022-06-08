package com.lidonghao.distributedlockdemo.client;

import com.lidonghao.distributedlockdemo.exception.LockException;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;

/**
 * @类名称 ZookeeperClient.java
 * @类描述 Zookeeper 客户端
 *
 */
public class ZookeeperClient {
    private ZooKeeper zooKeeper;
    /**
     * zk是一个目录结构，root为最外层目录
     */
    private String root = "/ldhlocks";

    protected void init(String baseUrl) throws LockException {
        try {
            final CountDownLatch countDownLatch = new CountDownLatch(1);
            zooKeeper = new ZooKeeper(baseUrl, 500_000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
                        //如果收到了服务端的响应事件，连接成功
                        countDownLatch.countDown();
                    }
                }
            });
            countDownLatch.await();
            Stat stat = zooKeeper.exists(root, false);
            if (null == stat) {
                // 创建根节点
                zooKeeper.create(root, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            throw new LockException("zookepper 初始化失败" + e.getMessage());
        }
    }

    private volatile static ZookeeperClient client;

    private ZookeeperClient(String baseUrl) throws LockException {
        init(baseUrl);
    }

    /**
     * 双锁单例
     */
    public static ZookeeperClient createInstance(String baseUrl) throws LockException {
        if (client == null) {
            synchronized (ZookeeperClient.class) {
                if (client == null) {
                    client = new ZookeeperClient(baseUrl);
                }
            }
        }
        return client;
    }

    /**
     * 创建临时子节点
     *
     * @param lockName
     * @param value
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String createTempNode(String lockName, String value) throws KeeperException, InterruptedException {

        String node = zooKeeper.create(root + "/" + lockName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL);
        return node;
    }

    /**
     * 获取所有锁node节点
     */
    public TreeSet<String> getAllLockNodes() throws KeeperException, InterruptedException {
        List<String> subNodes = zooKeeper.getChildren(root, false);
        TreeSet<String> sortedNodes = new TreeSet<>();
        for (String node : subNodes) {
            sortedNodes.add(root + "/" + node);
        }
        return sortedNodes;
    }

    public Stat existslowerNode(String lowerNode, ZookeeperLockWatcher lockWatcher) throws KeeperException, InterruptedException {
        return zooKeeper.exists(lowerNode, lockWatcher);
    }

    public void deleteNode(String nodeId) throws KeeperException, InterruptedException {
        zooKeeper.delete(nodeId, -1);
    }
}