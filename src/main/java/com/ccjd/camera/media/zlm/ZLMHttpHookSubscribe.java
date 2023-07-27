package com.ccjd.camera.media.zlm;

import com.alibaba.fastjson.JSONObject;
import com.ccjd.camera.media.zlm.dto.MediaServerItem;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description:针对 ZLMediaServer的hook事件订阅
 * @author: pan
 * @date:   2020年12月2日 21:17:32
 */
@Component
public class ZLMHttpHookSubscribe {

    public enum HookType{
        on_flow_report,
        on_http_access,
        on_play,
        on_publish,
        on_record_mp4,
        on_rtsp_auth,
        on_rtsp_realm,
        on_shell_login,
        on_stream_changed,
        on_stream_none_reader,
        on_stream_not_found,
        on_server_started,
        on_server_keepalive
    }

    @FunctionalInterface
    public interface Event{
        void response(MediaServerItem mediaServerItem, JSONObject response);
    }

    private Map<HookType, Map<JSONObject, Event>> allSubscribes = new ConcurrentHashMap<>();

    public void addSubscribe(HookType type, JSONObject hookResponse, Event event) {
        allSubscribes.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(hookResponse, event);
    }

    public Event getSubscribe(HookType type, JSONObject hookResponse) {
        Event event= null;
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        for (JSONObject key : eventMap.keySet()) {
            Boolean result = null;
            for (String s : key.keySet()) {
                if (result == null) {
                    result = key.getString(s).equals(hookResponse.getString(s));
                }else {
                    if (key.getString(s) == null) {
                        continue;
                    }
                    result = result && key.getString(s).equals(hookResponse.getString(s));
                }

            }
            if (null != result && result) {
                event = eventMap.get(key);
            }
        }
        return event;
    }

    public void removeSubscribe(HookType type, JSONObject hookResponse) {
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return;
        }

        Set<Map.Entry<JSONObject, Event>> entries = eventMap.entrySet();
        if (entries.size() > 0) {
            List<Map.Entry<JSONObject, Event>> entriesToRemove = new ArrayList<>();
            for (Map.Entry<JSONObject, Event> entry : entries) {
                JSONObject key = entry.getKey();
                Boolean result = null;
                for (String s : key.keySet()) {
                    if (result == null) {
                        result = key.getString(s).equals(hookResponse.getString(s));
                    }else {
                        if (key.getString(s) == null) continue;
                        result = result && key.getString(s).equals(hookResponse.getString(s));
                    }
                }
                if (null != result && result){
                    entriesToRemove.add(entry);
                }
            }

            if (!CollectionUtils.isEmpty(entriesToRemove)) {
                for (Map.Entry<JSONObject, Event> entry : entriesToRemove) {
                    entries.remove(entry);
                }
            }

        }
    }

    /**
     * 获取某个类型的所有的订阅
     * @param type
     * @return
     */
    public List<Event> getSubscribes(HookType type) {
        // ZLMHttpHookSubscribe.Event event= null;
        Map<JSONObject, Event> eventMap = allSubscribes.get(type);
        if (eventMap == null) {
            return null;
        }
        List<Event> result = new ArrayList<>();
        for (JSONObject key : eventMap.keySet()) {
            result.add(eventMap.get(key));
        }
        return result;
    }


}