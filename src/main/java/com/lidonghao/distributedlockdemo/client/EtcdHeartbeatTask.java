package com.lidonghao.distributedlockdemo.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * etcd 心跳任务：用于节点探活，节点重入
 */
public class EtcdHeartbeatTask extends Thread{

    private static final Logger LOGGER = LoggerFactory.getLogger(EtcdHeartbeatTask.class);

    private final EtcdClient etcdClient;

    EtcdHeartbeatTask(EtcdClient etcdClient) {
        this.etcdClient = etcdClient;
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(10);
                testAvailableEtcdNodes();
                testBrokenEtcdNodes();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @方法名称 testAvailableEtcdNodes
     * @功能描述 节点探活
     */
    private void testAvailableEtcdNodes() {
        List<String> availableEtcdNodes = new ArrayList<String>(etcdClient.getAvailableEtcdNodes());
        for (String etcdNode : availableEtcdNodes) {
            try {
                URI uri = new URI(etcdNode);
                testSocketConnect(uri.getHost(), uri.getPort());
            } catch (Exception e) {
                etcdClient.setBrokenEtcdNode(etcdNode);
                LOGGER.warn("Lock etcd node [{}] broken", etcdNode);
            }
        }
    }

    /**
     * @方法名称 testBrokenEtcdNodes
     * @功能描述 失联节点 重连
     */
    private void testBrokenEtcdNodes() {
        List<String> brokenEtcdNodes = new ArrayList<String>(etcdClient.getBrokenEtcdNodes());
        for (String etcdNode : brokenEtcdNodes) {
            try {
                URI uri = new URI(etcdNode);
                testSocketConnect(uri.getHost(), uri.getPort());
                etcdClient.setAvailableEtcdNode(etcdNode);
                LOGGER.info("Lock etcd node [{}] available", etcdNode);
            } catch (Exception e) {
            }
        }
    }

    /**
     * @方法名称 testSocketConnect
     * @功能描述 心跳链接
     * @param host ip
     * @param port 端口
     * @throws IOException 链接异常
     */
    private void testSocketConnect(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 100);
        socket.close();
    }
}
