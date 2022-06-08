package com.lidonghao.distributedlockdemo.client;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.concurrent.CountDownLatch;

/**
 * zk 锁的监听器
 */
public class ZookeeperLockWatcher implements Watcher {
    private CountDownLatch latch = null;

    public ZookeeperLockWatcher(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void process(WatchedEvent event) {

        if (event.getType() == Watcher.Event.EventType.NodeDeleted){
            latch.countDown();
        }
    }
}
