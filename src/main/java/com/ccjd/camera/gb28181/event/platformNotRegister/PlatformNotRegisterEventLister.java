package com.ccjd.camera.gb28181.event.platformNotRegister;

import com.ccjd.camera.conf.SipConfig;
import com.ccjd.camera.gb28181.bean.ParentPlatform;
import com.ccjd.camera.gb28181.bean.SendRtpItem;
import com.ccjd.camera.gb28181.event.SipSubscribe;
import com.ccjd.camera.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.ccjd.camera.media.zlm.ZLMRTPServerFactory;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import com.ccjd.camera.service.IMediaServerService;
import com.ccjd.camera.storager.IRedisCatchStorage;
import com.ccjd.camera.storager.IVideoManagerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @description: 平台未注册事件,来源有二:
 *               1、平台新添加
 *               2、平台心跳超时
 * @author: panll
 * @date: 2020年11月24日 10:00
 */
@Component
public class PlatformNotRegisterEventLister implements ApplicationListener<PlatformNotRegisterEvent> {

    private final static Logger logger = LoggerFactory.getLogger(PlatformNotRegisterEventLister.class);

    @Autowired
    private IVideoManagerStorage storager;
    @Autowired
    private IRedisCatchStorage redisCatchStorage;
    @Autowired
    private IMediaServerService mediaServerService;

    @Autowired
    private SIPCommanderFroPlatform sipCommanderFroPlatform;

    @Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

    @Autowired
    private SipConfig config;

    // @Autowired
    // private RedisUtil redis;

    @Override
    public void onApplicationEvent(PlatformNotRegisterEvent event) {

        logger.info("[ 平台未注册事件 ]平台国标ID：" + event.getPlatformGbID());

        ParentPlatform parentPlatform = storager.queryParentPlatByServerGBId(event.getPlatformGbID());
        if (parentPlatform == null) {
            logger.info("[ 平台未注册事件 ] 平台已经删除!!! 平台国标ID：" + event.getPlatformGbID());
            return;
        }
        // 查询是否有推流， 如果有则都停止
        List<SendRtpItem> sendRtpItems = redisCatchStorage.querySendRTPServer(event.getPlatformGbID());
        logger.info("[ 平台未注册事件 ] 停止[ {} ]的所有推流size", sendRtpItems.size());
        if (sendRtpItems != null && sendRtpItems.size() > 0) {
            logger.info("[ 平台未注册事件 ] 停止[ {} ]的所有推流", event.getPlatformGbID());
            for (SendRtpItem sendRtpItem : sendRtpItems) {
                redisCatchStorage.deleteSendRTPServer(event.getPlatformGbID(), sendRtpItem.getChannelId(), null, null);
                MediaServerItem mediaInfo = mediaServerService.getOne(sendRtpItem.getMediaServerId());
                Map<String, Object> param = new HashMap<>();
                param.put("vhost", "__defaultVhost__");
                param.put("app", sendRtpItem.getApp());
                param.put("stream", sendRtpItem.getStreamId());
                zlmrtpServerFactory.stopSendRtpStream(mediaInfo, param);
            }

        }
        Timer timer = new Timer();
        SipSubscribe.Event okEvent = (responseEvent)->{
            timer.cancel();
        };
        logger.info("[平台注册]平台国标ID：" + event.getPlatformGbID());
        sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
        // 设置注册失败则每隔15秒发起一次注册
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("[平台注册]再次向平台注册，平台国标ID：" + event.getPlatformGbID());
                sipCommanderFroPlatform.register(parentPlatform, null, okEvent);
            }
        }, config.getRegisterTimeInterval()* 1000, config.getRegisterTimeInterval()* 1000);//十五秒后再次发起注册
    }
}