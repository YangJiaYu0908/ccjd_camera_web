package com.ccjd.camera.service.bean;

import com.ccjd.camera.gb28181.transmit.callback.RequestMessage;

public interface PlayBackCallback {

    void call(PlayBackResult<RequestMessage> msg);

}
