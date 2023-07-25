package com.ccjd.camera.service;

import com.ccjd.camera.common.StreamInfo;
import com.ccjd.camera.conf.exception.ServiceException;
import com.ccjd.camera.gb28181.bean.Device;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import com.ccjd.camera.service.bean.ErrorCallback;
import com.ccjd.camera.service.bean.SSRCInfo;

import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * 点播处理
 */
public interface IPlayService {

    void play(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, Device device, String channelId,
              ErrorCallback<Object> callback);
    SSRCInfo play(MediaServerItem mediaServerItem, String deviceId, String channelId, String ssrc, ErrorCallback<Object> callback);

    MediaServerItem getNewMediaServerItem(Device device);

    /**
     * 获取包含assist服务的节点
     */
    MediaServerItem getNewMediaServerItemHasAssist(Device device);

    void playBack(String deviceId, String channelId, String startTime, String endTime, ErrorCallback<Object> callback);
    void playBack(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo, String deviceId, String channelId, String startTime, String endTime, ErrorCallback<Object> callback);

    void zlmServerOffline(String mediaServerId);

    void download(String deviceId, String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback);
    void download(MediaServerItem mediaServerItem, SSRCInfo ssrcInfo,String deviceId,  String channelId, String startTime, String endTime, int downloadSpeed, ErrorCallback<Object> callback);

    StreamInfo getDownLoadInfo(String deviceId, String channelId, String stream);

    void zlmServerOnline(String mediaServerId);

    void pauseRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException;

    void resumeRtp(String streamId) throws ServiceException, InvalidArgumentException, ParseException, SipException;

    void getSnap(String deviceId, String channelId, String fileName, ErrorCallback errorCallback);
}
