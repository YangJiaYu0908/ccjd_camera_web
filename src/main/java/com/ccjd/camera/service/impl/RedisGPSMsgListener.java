package com.ccjd.camera.service.impl;

import com.alibaba.fastjson.JSON;
import com.ccjd.camera.service.bean.GPSMsgInfo;
import com.ccjd.camera.storager.IRedisCatchStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class RedisGPSMsgListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(RedisGPSMsgListener.class);

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        logger.info("收到来自REDIS的GPS通知： {}", new String(message.getBody()));
        GPSMsgInfo gpsMsgInfo = JSON.parseObject(message.getBody(), GPSMsgInfo.class);
        redisCatchStorage.updateGpsMsgInfo(gpsMsgInfo);
    }
}
