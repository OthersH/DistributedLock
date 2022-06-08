package com.lidonghao.distributedlockdemo.client;

import com.alibaba.fastjson.JSON;

import java.util.List;

/**
 *etcd 节点数据对象
 */
public class EtcdNodeData {

    public String key;

    public long createdIndex;

    public long modifiedIndex;

    public String value;

    public String expiration;

    public Integer ttl;

    public boolean dir;

    public List<EtcdNodeData> nodes;

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
