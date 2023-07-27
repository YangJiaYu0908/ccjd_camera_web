package com.ccjd.camera.gb28181.transmit.event.request.impl.message.query.cmd;

import com.ccjd.camera.conf.SipConfig;
import com.ccjd.camera.gb28181.bean.Device;
import com.ccjd.camera.gb28181.bean.ParentPlatform;
import com.ccjd.camera.gb28181.event.EventPublisher;
import com.ccjd.camera.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.ccjd.camera.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.ccjd.camera.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.ccjd.camera.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.ccjd.camera.storager.IVideoManagerStorage;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.message.Response;
import java.text.ParseException;

@Component
public class AlarmQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private Logger logger = LoggerFactory.getLogger(AlarmQueryMessageHandler.class);
    private final String cmdType = "Alarm";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private IVideoManagerStorage storager;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Autowired
    private SipConfig config;

    @Autowired
    private EventPublisher publisher;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        logger.info("不支持alarm查询");
        try {
            responseAck(evt, Response.NOT_FOUND, "not support alarm query");
        } catch (SipException e) {
            e.printStackTrace();
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }
}
