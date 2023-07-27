package com.ccjd.camera.gb28181.event;

import com.ccjd.camera.common.VideoManagerConstants;
import com.ccjd.camera.conf.UserSetting;
import com.ccjd.camera.utils.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**    
 * @description:设备离在线状态检测器，用于检测设备状态
 * @author: swwheihei
 * @date:   2020年5月13日 下午2:40:29     
 */
@Component
public class DeviceOffLineDetector {

	@Autowired
    private RedisUtil redis;

	@Autowired
    private UserSetting userSetting;
	
	public boolean isOnline(String deviceId) {
		String key = VideoManagerConstants.KEEPLIVEKEY_PREFIX + userSetting.getServerId() + "_" + deviceId;
		return redis.hasKey(key);
	}
}
