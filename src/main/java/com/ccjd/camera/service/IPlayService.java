package com.ccjd.camera.service;

import com.alibaba.fastjson.JSONObject;
import com.ccjd.camera.common.StreamInfo;
import com.ccjd.camera.gb28181.bean.Device;
import com.ccjd.camera.gb28181.bean.InviteStreamCallback;
import com.ccjd.camera.gb28181.bean.InviteStreamInfo;
import com.ccjd.camera.gb28181.event.SipSubscribe;
import com.ccjd.camera.media.zlm.ZLMHttpHookSubscribe;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import com.ccjd.camera.service.bean.InviteTimeOutCallback;
import com.ccjd.camera.service.bean.PlayBackCallback;
import com.ccjd.camera.service.bean.SSRCInfo;
import com.ccjd.camera.vmanager.gb28181.play.bean.PlayResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * 点播处理
 */
public interface IPlayService {

    void onPublishHandlerForPlay(MediaServerItem mediaServerItem, JSONObject resonse, String deviceId, String channelId, String uuid);

    void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
              ZLMHttpHookSubscribe.Event hookEvent, SipSubscribe.Event errorEvent,
              InviteTimeOutCallback timeoutCallback, String uuid);
    PlayResult play(MediaServerItem mediaServerItem, String deviceId, String channelId, ZLMHttpHookSubscribe.Event event, SipSubscribe.Event errorEvent, Runnable timeoutCallback);

    MediaServerItem getNewMediaServerItem(Device device);

    void onPublishHandlerForDownload(InviteStreamInfo inviteStreamInfo, String deviceId, String channelId, String toString);

    DeferredResult<ResponseEntity<String>> playBack(String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<ResponseEntity<String>> playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId, String channelId, String startTime, String endTime, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    void zlmServerOffline(String mediaServerId);

    DeferredResult<ResponseEntity<String>> download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);
    DeferredResult<ResponseEntity<String>> download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId,  String channelId, String startTime, String endTime, int downloadSpeed, InviteStreamCallback infoCallBack, PlayBackCallback hookCallBack);

    StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream);
}
