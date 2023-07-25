package com.ccjd.camera.gb28181.task;

import com.alibaba.fastjson2.JSONObject;
import com.ccjd.camera.conf.UserSetting;
import com.ccjd.camera.gb28181.bean.Device;
import com.ccjd.camera.gb28181.bean.ParentPlatform;
import com.ccjd.camera.gb28181.bean.SendRtpItem;
import com.ccjd.camera.gb28181.session.SSRCFactory;
import com.ccjd.camera.gb28181.transmit.cmd.ISIPCommanderForPlatform;
import com.ccjd.camera.media.zlm.ZLMRESTfulUtils;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import com.ccjd.camera.service.IDeviceService;
import com.ccjd.camera.service.IMediaServerService;
import com.ccjd.camera.service.IPlatformService;
import com.ccjd.camera.storager.IRedisCatchStorage;
import com.ccjd.camera.storager.IVideoManagerStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 系统启动时控制设备
 * @author lin
 */
@Component
@Order(value=14)
public class SipRunner implements CommandLineRunner {

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private IRedisCatchStorage redisCatchStorage;

    @Autowired
    private SSRCFactory ssrcFactory;

    @Autowired
    private UserSetting userSetting;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ZLMRESTfulUtils zlmresTfulUtils;

    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private IPlatformService platformService;

    @Autowired
    private ISIPCommanderForPlatform commanderForPlatform;

    @Override
    public void run(String... args) throws Exception {
        List<Device> deviceList = deviceService.getAllOnlineDevice();

        for (Device device : deviceList) {
            if (deviceService.expire(device)){
                deviceService.offline(device.getDeviceId(), "注册已过期");
            }else {
                deviceService.online(device, null);
            }
        }
        // 重置cseq计数
        redisCatchStorage.resetAllCSEQ();
        // 清理redis
        // 清理数据库不存在但是redis中存在的数据
        List<Device> devicesInDb = deviceService.getAll();
        if (devicesInDb.size() == 0) {
            redisCatchStorage.removeAllDevice();
        }else {
            List<Device> devicesInRedis = redisCatchStorage.getAllDevices();
            if (devicesInRedis.size() > 0) {
                Map<String, Device> deviceMapInDb = new HashMap<>();
                devicesInDb.parallelStream().forEach(device -> {
                    deviceMapInDb.put(device.getDeviceId(), device);
                });
                devicesInRedis.parallelStream().forEach(device -> {
                    if (deviceMapInDb.get(device.getDeviceId()) == null) {
                        redisCatchStorage.removeDevice(device.getDeviceId());
                    }
                });
            }
        }


        // 查找国标推流
        List<SendRtpItem> sendRtpItems = redisCatchStorage.queryAllSendRTPServer();
        if (sendRtpItems.size() > 0) {
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                MediaServerItem mediaServerItem = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                redisCatchStorage.deleteSendRTPServer(sendRtpItem.getPlatformId(),sendRtpItem.getChannelId(), sendRtpItem.getCallId(),sendRtpItem.getStreamId());
                if (mediaServerItem != null) {
                    ssrcFactory.releaseSsrc(sendRtpItem.getMediaServerId(), sendRtpItem.getSsrc());
                    Map<String, Object> param = new HashMap<>();
                    param.put("vhost","__defaultVhost__");
                    param.put("app",sendRtpItem.getApp());
                    param.put("stream",sendRtpItem.getStreamId());
                    param.put("ssrc",sendRtpItem.getSsrc());
                    JSONObject jsonObject = zlmresTfulUtils.stopSendRtp(mediaServerItem, param);
                    if (jsonObject != null && jsonObject.getInteger("code") == 0) {
                        ParentPlatform platform = platformService.queryPlatformByServerGBId(sendRtpItem.getPlatformId());
                        if (platform != null) {
                            commanderForPlatform.streamByeCmd(platform, sendRtpItem.getCallId());
                        }
                    }
                }
            }
        }
    }
}
