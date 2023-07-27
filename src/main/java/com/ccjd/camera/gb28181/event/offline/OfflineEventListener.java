package com.ccjd.camera.gb28181.event.offline;

import com.ccjd.camera.common.VideoManagerConstants;
import com.ccjd.camera.conf.UserSetting;
import com.ccjd.camera.gb28181.bean.DeviceChannel;
import com.ccjd.camera.gb28181.bean.SsrcTransaction;
import com.ccjd.camera.gb28181.event.EventPublisher;
import com.ccjd.camera.gb28181.event.subscribe.catalog.CatalogEvent;
import com.ccjd.camera.gb28181.session.VideoStreamSessionManager;
import com.ccjd.camera.media.zlm.ZLMRTPServerFactory;
import com.ccjd.camera.service.IMediaServerService;
import com.ccjd.camera.storager.IVideoManagerStorage;
import com.ccjd.camera.utils.redis.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: 离线事件监听器，监听到离线后，修改设备离在线状态。 设备离线有两个来源：
 *               1、设备主动注销，发送注销指令，{@link com.ccjd.camera.gb28181.transmit.event.request.impl.RegisterRequestProcessor}
 *               2、设备未知原因离线，心跳超时,{@link com.ccjd.camera.gb28181.event.offline.OfflineEventListener}
 * @author: swwheihei
 * @date: 2020年5月6日 下午1:51:23
 */
@Component
public class OfflineEventListener implements ApplicationListener<OfflineEvent> {

	private final static Logger logger = LoggerFactory.getLogger(OfflineEventListener.class);
	
	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private VideoStreamSessionManager streamSession;
	
	@Autowired
    private RedisUtil redis;

	@Autowired
    private UserSetting userSetting;

	@Autowired
    private EventPublisher eventPublisher;


	@Autowired
    private IMediaServerService mediaServerService;


	@Autowired
    private ZLMRTPServerFactory zlmrtpServerFactory;

	@Override
	public void onApplicationEvent(OfflineEvent event) {

		logger.info("设备离线事件触发，deviceId：" + event.getDeviceId() + ",from:" + event.getFrom());

		String key = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetting.getServerId() + "_" + event.getDeviceId();

		switch (event.getFrom()) {
			// 心跳超时触发的离线事件，说明redis中已删除，无需处理
			case VideoManagerConstants.EVENT_OUTLINE_TIMEOUT:
				break;
			// 设备主动注销触发的离线事件，需要删除redis中的超时监听
			case VideoManagerConstants.EVENT_OUTLINE_UNREGISTER:
				redis.del(key);
				break;
			default:
				boolean exist = redis.hasKey(key);
				if (exist) {
					redis.del(key);
				}
		}

		List<DeviceChannel> deviceChannelList = storager.queryOnlineChannelsByDeviceId(event.getDeviceId());
		eventPublisher.catalogEventPublish(null, deviceChannelList, CatalogEvent.OFF);
		// 处理离线监听
		storager.outline(event.getDeviceId());

		// TODO 离线取消订阅

		// 离线释放所有ssrc
		List<SsrcTransaction> ssrcTransactions = streamSession.getSsrcTransactionForAll(event.getDeviceId(), null, null, null);
		if (ssrcTransactions != null && ssrcTransactions.size() > 0) {
			for (SsrcTransaction ssrcTransaction : ssrcTransactions) {
				mediaServerService.releaseSsrc(ssrcTransaction.getMediaServerId(), ssrcTransaction.getSsrc());
				mediaServerService.closeRTPServer(event.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
				streamSession.remove(event.getDeviceId(), ssrcTransaction.getChannelId(), ssrcTransaction.getStream());
			}
		}

	}
}