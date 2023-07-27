package com.ccjd.camera.service.bean;

public interface PlayBackCallback<T> {

    void call(PlayBackResult<T> msg);

}
