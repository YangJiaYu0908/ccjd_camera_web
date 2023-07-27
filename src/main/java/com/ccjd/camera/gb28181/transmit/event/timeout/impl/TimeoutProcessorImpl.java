package com.ccjd.camera.gb28181.transmit.event.timeout.impl;

import com.ccjd.camera.gb28181.event.SipSubscribe;
import com.ccjd.camera.gb28181.transmit.SIPProcessorObserver;
import com.ccjd.camera.gb28181.transmit.event.timeout.ITimeoutProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.TimeoutEvent;
import javax.sip.header.CallIdHeader;

@Component
public class TimeoutProcessorImpl implements InitializingBean, ITimeoutProcessor {

    @Autowired
    private SIPProcessorObserver processorObserver;

    @Autowired
    private SipSubscribe sipSubscribe;

    @Override
    public void afterPropertiesSet() throws Exception {
        processorObserver.addTimeoutProcessor(this);
    }

    @Override
    public void process(TimeoutEvent event) {
        // TODO Auto-generated method stub
        CallIdHeader callIdHeader = event.getClientTransaction().getDialog().getCallId();
        String callId = callIdHeader.getCallId();
        SipSubscribe.Event errorSubscribe = sipSubscribe.getErrorSubscribe(callId);
        SipSubscribe.EventResult<TimeoutEvent> timeoutEventEventResult = new SipSubscribe.EventResult<>(event);
        errorSubscribe.response(timeoutEventEventResult);
    }
}
