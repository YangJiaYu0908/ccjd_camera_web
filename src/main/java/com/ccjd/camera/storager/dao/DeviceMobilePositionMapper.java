package com.ccjd.camera.storager.dao;

import com.ccjd.camera.gb28181.bean.MobilePosition;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
//import org.springframework.stereotype.Repository;

@Mapper
//@Repository
public interface DeviceMobilePositionMapper {

    @Insert("INSERT INTO device_mobile_position (deviceId,channelId, deviceName, time, longitude, latitude, altitude, speed, direction, reportSource, geodeticSystem, cnLng, cnLat) " +
            "VALUES ('${deviceId}','${channelId}', '${deviceName}', '${time}', ${longitude}, ${latitude}, ${altitude}, ${speed}, ${direction}, '${reportSource}', '${geodeticSystem}', '${cnLng}', '${cnLat}')")
    int insertNewPosition(MobilePosition mobilePosition);

    @Select(value = {" <script>" +
    "SELECT * FROM device_mobile_position" +
    " WHERE deviceId = #{deviceId} and channelId = #{channelId} " +
    "<if test=\"startTime != null\"> AND time&gt;=#{startTime}</if>" +
    "<if test=\"endTime != null\"> AND time&lt;=#{endTime}</if>" +
    " ORDER BY time ASC" +
    " </script>"})
    List<MobilePosition> queryPositionByDeviceIdAndTime(String deviceId, String channelId, String startTime, String endTime);

    @Select("SELECT * FROM device_mobile_position WHERE deviceId = #{deviceId}" +
            " ORDER BY time DESC LIMIT 1")
    MobilePosition queryLatestPositionByDevice(String deviceId);

    @Delete("DELETE FROM device_mobile_position WHERE deviceId = #{deviceId}")
    int clearMobilePositionsByDeviceId(String deviceId);

}
