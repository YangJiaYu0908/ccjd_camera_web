package com.ccjd.camera.vmanager.gb28181.play;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ccjd.camera.common.InviteInfo;
import com.ccjd.camera.common.InviteSessionStatus;
import com.ccjd.camera.common.InviteSessionType;
import com.ccjd.camera.common.StreamInfo;
import com.ccjd.camera.conf.UserSetting;
import com.ccjd.camera.conf.exception.ControllerException;
import com.ccjd.camera.conf.exception.SsrcTransactionNotFoundException;
import com.ccjd.camera.gb28181.bean.Device;
import com.ccjd.camera.gb28181.bean.SsrcTransaction;
import com.ccjd.camera.gb28181.session.VideoStreamSessionManager;
import com.ccjd.camera.gb28181.transmit.callback.DeferredResultHolder;
import com.ccjd.camera.gb28181.transmit.callback.RequestMessage;
import com.ccjd.camera.gb28181.transmit.cmd.impl.SIPCommander;
import com.ccjd.camera.media.zlm.ZLMRESTfulUtils;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import com.ccjd.camera.service.IInviteStreamService;
import com.ccjd.camera.service.IMediaServerService;
import com.ccjd.camera.service.IMediaService;
import com.ccjd.camera.service.IPlayService;
import com.ccjd.camera.service.bean.InviteErrorCode;
import com.ccjd.camera.storager.IRedisCatchStorage;
import com.ccjd.camera.storager.IVideoManagerStorage;
import com.ccjd.camera.utils.DateUtil;
import com.ccjd.camera.vmanager.bean.ErrorCode;
import com.ccjd.camera.vmanager.bean.StreamContent;
import com.ccjd.camera.vmanager.bean.WVPResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;

@Tag(name  = "国标设备点播")

@RestController
@RequestMapping("/api/play")
public class PlayController {

	private final static Logger logger = LoggerFactory.getLogger(PlayController.class);

	@Autowired
	private SIPCommander cmder;

	@Autowired
	private VideoStreamSessionManager streamSession;

	@Autowired
	private IVideoManagerStorage storager;

	@Autowired
	private IRedisCatchStorage redisCatchStorage;

	@Autowired
	private IInviteStreamService inviteStreamService;

	@Autowired
	private ZLMRESTfulUtils zlmresTfulUtils;

	@Autowired
	private DeferredResultHolder resultHolder;

	@Autowired
	private IPlayService playService;

	@Autowired
	private IMediaService mediaService;

	@Autowired
	private IMediaServerService mediaServerService;

	@Autowired
	private UserSetting userSetting;

	@Operation(summary = "开始点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/start/{deviceId}/{channelId}")
	public DeferredResult<WVPResult<StreamContent>> play(HttpServletRequest request, @PathVariable String deviceId,
														 @PathVariable String channelId) {

		// 获取可用的zlm
		Device device = storager.queryVideoDevice(deviceId);
		MediaServerItem newMediaServerItem = playService.getNewMediaServerItem(device);

		RequestMessage requestMessage = new RequestMessage();
		String key = DeferredResultHolder.CALLBACK_CMD_PLAY + deviceId + channelId;
		requestMessage.setKey(key);
		String uuid = UUID.randomUUID().toString();
		requestMessage.setId(uuid);
		DeferredResult<WVPResult<StreamContent>> result = new DeferredResult<>(userSetting.getPlayTimeout().longValue());

		result.onTimeout(()->{
			logger.info("点播接口等待超时");
			// 释放rtpserver
			WVPResult<StreamInfo> wvpResult = new WVPResult<>();
			wvpResult.setCode(ErrorCode.ERROR100.getCode());
			wvpResult.setMsg("点播超时");
			requestMessage.setData(wvpResult);
			resultHolder.invokeResult(requestMessage);
		});

		// 录像查询以channelId作为deviceId查询
		resultHolder.put(key, uuid, result);

		playService.play(newMediaServerItem, deviceId, channelId, (code, msg, data) -> {
			WVPResult<StreamContent> wvpResult = new WVPResult<>();
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				wvpResult.setCode(ErrorCode.SUCCESS.getCode());
				wvpResult.setMsg(ErrorCode.SUCCESS.getMsg());

				if (data != null) {
					StreamInfo streamInfo = (StreamInfo)data;
					if (userSetting.getUseSourceIpAsStreamIp()) {
						streamInfo.channgeStreamIp(request.getLocalName());
					}
					wvpResult.setData(new StreamContent(streamInfo));
				}
			}else {
				wvpResult.setCode(code);
				wvpResult.setMsg(msg);
			}
			requestMessage.setData(wvpResult);
			resultHolder.invokeResult(requestMessage);
		});
		return result;
	}

	@Operation(summary = "停止点播")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/stop/{deviceId}/{channelId}")
	public JSONObject playStop(@PathVariable String deviceId, @PathVariable String channelId) {

		logger.debug(String.format("设备预览/回放停止API调用，streamId：%s_%s", deviceId, channelId ));

		if (deviceId == null || channelId == null) {
			throw new ControllerException(ErrorCode.ERROR400);
		}

		Device device = storager.queryVideoDevice(deviceId);
		if (device == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "设备[" + deviceId + "]不存在");
		}

		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);
		if (inviteInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "点播未找到");
		}
		if (InviteSessionStatus.ok == inviteInfo.getStatus()) {
			try {
				logger.warn("[停止点播] {}/{}", device.getDeviceId(), channelId);
				cmder.streamByeCmd(device, channelId, inviteInfo.getStream(), null, null);
			} catch (InvalidArgumentException | SipException | ParseException | SsrcTransactionNotFoundException e) {
				logger.error("[命令发送失败] 停止点播， 发送BYE: {}", e.getMessage());
				throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
			}
		}
		inviteStreamService.removeInviteInfoByDeviceAndChannel(InviteSessionType.PLAY, deviceId, channelId);

		storager.stopPlay(deviceId, channelId);
		JSONObject json = new JSONObject();
		json.put("deviceId", deviceId);
		json.put("channelId", channelId);
		return json;
	}

	/**
	 * 将不是h264的视频通过ffmpeg 转码为h264 + aac
	 * @param streamId 流ID
	 */
	@Operation(summary = "将不是h264的视频通过ffmpeg 转码为h264 + aac")
	@Parameter(name = "streamId", description = "视频流ID", required = true)
	@PostMapping("/convert/{streamId}")
	public JSONObject playConvert(@PathVariable String streamId) {
//		StreamInfo streamInfo = redisCatchStorage.queryPlayByStreamId(streamId);

		InviteInfo inviteInfo = inviteStreamService.getInviteInfoByStream(null, streamId);
		if (inviteInfo == null || inviteInfo.getStreamInfo() == null) {
			logger.warn("视频转码API调用失败！, 视频流已经停止!");
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到视频流信息, 视频流可能已经停止");
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(inviteInfo.getStreamInfo().getMediaServerId());
		JSONObject rtpInfo = zlmresTfulUtils.getRtpInfo(mediaInfo, streamId);
		if (!rtpInfo.getBoolean("exist")) {
			logger.warn("视频转码API调用失败！, 视频流已停止推流!");
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "未找到视频流信息, 视频流可能已停止推流");
		} else {
			String dstUrl = String.format("rtmp://%s:%s/convert/%s", "127.0.0.1", mediaInfo.getRtmpPort(),
					streamId );
			String srcUrl = String.format("rtsp://%s:%s/rtp/%s", "127.0.0.1", mediaInfo.getRtspPort(), streamId);
			JSONObject jsonObject = zlmresTfulUtils.addFFmpegSource(mediaInfo, srcUrl, dstUrl, "1000000", true, false, null);
			logger.info(jsonObject.toJSONString());
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				JSONObject data = jsonObject.getJSONObject("data");
				if (data != null) {
					JSONObject result = new JSONObject();
					result.put("key", data.getString("key"));
					StreamInfo streamInfoResult = mediaService.getStreamInfoByAppAndStreamWithCheck("convert", streamId, mediaInfo.getId(), false);
					result.put("StreamInfo", streamInfoResult);
					return result;
				}else {
					throw new ControllerException(ErrorCode.ERROR100.getCode(), "转码失败");
				}
			}else {
				throw new ControllerException(ErrorCode.ERROR100.getCode(), "转码失败");
			}
		}
	}

	/**
	 * 结束转码
	 */
	@Operation(summary = "结束转码")
	@Parameter(name = "key", description = "视频流key", required = true)
	@Parameter(name = "mediaServerId", description = "流媒体服务ID", required = true)
	@PostMapping("/convertStop/{key}")
	public void playConvertStop(@PathVariable String key, String mediaServerId) {
		if (mediaServerId == null) {
			throw new ControllerException(ErrorCode.ERROR400.getCode(), "流媒体：" + mediaServerId + "不存在" );
		}
		MediaServerItem mediaInfo = mediaServerService.getOne(mediaServerId);
		if (mediaInfo == null) {
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "使用的流媒体已经停止运行" );
		}else {
			JSONObject jsonObject = zlmresTfulUtils.delFFmpegSource(mediaInfo, key);
			logger.info(jsonObject.toJSONString());
			if (jsonObject != null && jsonObject.getInteger("code") == 0) {
				JSONObject data = jsonObject.getJSONObject("data");
				if (data == null || data.getBoolean("flag") == null || !data.getBoolean("flag")) {
					throw new ControllerException(ErrorCode.ERROR100 );
				}
			}else {
				throw new ControllerException(ErrorCode.ERROR100 );
			}
		}
	}

	@Operation(summary = "语音广播命令")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
    @GetMapping("/broadcast/{deviceId}")
    @PostMapping("/broadcast/{deviceId}")
    public DeferredResult<String> broadcastApi(@PathVariable String deviceId) {
        if (logger.isDebugEnabled()) {
            logger.debug("语音广播API调用");
        }
        Device device = storager.queryVideoDevice(deviceId);
		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		String key  = DeferredResultHolder.CALLBACK_CMD_BROADCAST + deviceId;
		if (resultHolder.exist(key, null)) {
			result.setResult("设备使用中");
			return result;
		}
		String uuid  = UUID.randomUUID().toString();
        if (device == null) {

			resultHolder.put(key, key,  result);
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Description", "Device 不存在");
			msg.setData(json);
			resultHolder.invokeResult(msg);
			return result;
		}
		try {
			cmder.audioBroadcastCmd(device, (event) -> {
				RequestMessage msg = new RequestMessage();
				msg.setKey(key);
				msg.setId(uuid);
				JSONObject json = new JSONObject();
				json.put("DeviceID", deviceId);
				json.put("CmdType", "Broadcast");
				json.put("Result", "Failed");
				json.put("Description", String.format("语音广播操作失败，错误码： %s, %s", event.statusCode, event.msg));
				msg.setData(json);
				resultHolder.invokeResult(msg);
			});
		} catch (InvalidArgumentException | SipException | ParseException e) {
			logger.error("[命令发送失败] 语音广播: {}", e.getMessage());
			throw new ControllerException(ErrorCode.ERROR100.getCode(), "命令发送失败: " + e.getMessage());
		}

		result.onTimeout(() -> {
			logger.warn("语音广播操作超时, 设备未返回应答指令");
			RequestMessage msg = new RequestMessage();
			msg.setKey(key);
			msg.setId(uuid);
			JSONObject json = new JSONObject();
			json.put("DeviceID", deviceId);
			json.put("CmdType", "Broadcast");
			json.put("Result", "Failed");
			json.put("Error", "Timeout. Device did not response to broadcast command.");
			msg.setData(json);
			resultHolder.invokeResult(msg);
		});
		resultHolder.put(key, uuid, result);
		return result;
	}

	@Operation(summary = "获取所有的ssrc")
	@GetMapping("/ssrc")
	public JSONObject getSSRC() {
		if (logger.isDebugEnabled()) {
			logger.debug("获取所有的ssrc");
		}
		JSONArray objects = new JSONArray();
		List<SsrcTransaction> allSsrc = streamSession.getAllSsrc();
		for (SsrcTransaction transaction : allSsrc) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("deviceId", transaction.getDeviceId());
			jsonObject.put("channelId", transaction.getChannelId());
			jsonObject.put("ssrc", transaction.getSsrc());
			jsonObject.put("streamId", transaction.getStream());
			objects.add(jsonObject);
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("data", objects);
		jsonObject.put("count", objects.size());
		return jsonObject;
	}

	@Operation(summary = "获取截图")
	@Parameter(name = "deviceId", description = "设备国标编号", required = true)
	@Parameter(name = "channelId", description = "通道国标编号", required = true)
	@GetMapping("/snap")
	public DeferredResult<String> getSnap(String deviceId, String channelId) {
		if (logger.isDebugEnabled()) {
			logger.debug("获取截图: {}/{}", deviceId, channelId);
		}

		DeferredResult<String> result = new DeferredResult<>(3 * 1000L);
		String key  = DeferredResultHolder.CALLBACK_CMD_SNAP + deviceId;
		String uuid  = UUID.randomUUID().toString();
		resultHolder.put(key, uuid,  result);

		RequestMessage message = new RequestMessage();
		message.setKey(key);
		message.setId(uuid);

		String fileName = deviceId + "_" + channelId + "_" + DateUtil.getNowForUrl() + "jpg";
		playService.getSnap(deviceId, channelId, fileName, (code, msg, data) -> {
			if (code == InviteErrorCode.SUCCESS.getCode()) {
				message.setData(data);
			}else {
				message.setData(WVPResult.fail(code, msg));
			}
			resultHolder.invokeResult(message);
		});
		return result;
	}

}

