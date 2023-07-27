package com.ccjd.camera.gb28181.bean;

import com.ccjd.camera.common.VideoManagerConstants;
import com.ccjd.camera.conf.DynamicTask;
import com.ccjd.camera.gb28181.task.impl.MobilePositionSubscribeHandlerTask;
import com.ccjd.camera.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.ccjd.camera.storager.IRedisCatchStorage;
import com.ccjd.camera.storager.IVideoManagerStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubscribeHolder {

    @Autowired
    private DynamicTask dynamicTask;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private ISIPCommanderForPlatform sipCommanderForPlatform;

    @Autowired
    private IVideoManagerStorage storager;

    private final String taskOverduePrefix = "subscribe_overdue_";

    private static ConcurrentHashMap<String, SubscribeInfo> catalogMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, SubscribeInfo> mobilePositionMap = new ConcurrentHashMap<>();


    public void putCatalogSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        catalogMap.put(platformId, subscribeInfo);
        // 添加订阅到期
        String taskOverdueKey = taskOverduePrefix +  "catalog_" + platformId;
        dynamicTask.stop(taskOverdueKey);
        // 添加任务处理订阅过期
        dynamicTask.startDelay(taskOverdueKey, () -> removeCatalogSubscribe(subscribeInfo.getId()),
                subscribeInfo.getExpires() * 1000);
    }

    public SubscribeInfo getCatalogSubscribe(String platformId) {
        return catalogMap.get(platformId);
    }

    public void removeCatalogSubscribe(String platformId) {
        catalogMap.remove(platformId);
        String taskOverdueKey = taskOverduePrefix +  "catalog_" + platformId;
        // 添加任务处理订阅过期
        dynamicTask.stop(taskOverdueKey);
    }

    public void putMobilePositionSubscribe(String platformId, SubscribeInfo subscribeInfo) {
        mobilePositionMap.put(platformId, subscribeInfo);
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX +  "MobilePosition_" + platformId;
        // 添加任务处理GPS定时推送
        dynamicTask.startCron(key, new MobilePositionSubscribeHandlerTask(redisCatchStorage, sipCommanderForPlatform, storager,  platformId, subscribeInfo.getSn(), key, this), subscribeInfo.getGpsInterval());
        String taskOverdueKey = taskOverduePrefix +  "MobilePosition_" + platformId;
        dynamicTask.stop(taskOverdueKey);
        // 添加任务处理订阅过期
        dynamicTask.startDelay(taskOverdueKey, () -> {
                    System.out.println("订阅过期");
                    removeMobilePositionSubscribe(subscribeInfo.getId());
                },
                subscribeInfo.getExpires() * 1000);
    }

    public SubscribeInfo getMobilePositionSubscribe(String platformId) {
        return mobilePositionMap.get(platformId);
    }

    public void removeMobilePositionSubscribe(String platformId) {
        mobilePositionMap.remove(platformId);
        String key = VideoManagerConstants.SIP_SUBSCRIBE_PREFIX +  "MobilePosition_" + platformId;
        // 结束任务处理GPS定时推送
        dynamicTask.stop(key);
        String taskOverdueKey = taskOverduePrefix +  "MobilePosition_" + platformId;
        // 添加任务处理订阅过期
        dynamicTask.stop(taskOverdueKey);
    }

    public List<String> getAllCatalogSubscribePlatform() {
        List<String> platforms = new ArrayList<>();
        if(catalogMap.size() > 0) {
            for (String key : catalogMap.keySet()) {
                platforms.add(catalogMap.get(key).getId());
            }
        }
        return platforms;
    }

    public void removeAllSubscribe(String platformId) {
        removeMobilePositionSubscribe(platformId);
        removeCatalogSubscribe(platformId);
    }
}
