package com.ccjd.camera.gb28181.task.impl;

import com.ccjd.camera.gb28181.bean.*;
import com.ccjd.camera.gb28181.task.ISubscribeTask;
import com.ccjd.camera.service.IPlatformService;
import com.ccjd.camera.utils.SpringBeanFactory;

/**
 * 向已经订阅(移动位置)的上级发送MobilePosition消息
 * @author lin
 */
public class MobilePositionSubscribeHandlerTask implements ISubscribeTask {


    private IPlatformService platformService;
    private String platformId;


    public MobilePositionSubscribeHandlerTask(String platformId) {
        this.platformService = SpringBeanFactory.getBean("platformServiceImpl");
        this.platformId = platformId;
    }

    @Override
    public void run() {
        platformService.sendNotifyMobilePosition(this.platformId);
    }

    @Override
    public void stop() {

    }
}
