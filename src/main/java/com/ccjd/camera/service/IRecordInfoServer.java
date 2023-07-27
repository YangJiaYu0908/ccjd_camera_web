package com.ccjd.camera.service;

import com.ccjd.camera.storager.dao.dto.RecordInfo;
import com.github.pagehelper.PageInfo;

public interface IRecordInfoServer {
    PageInfo<RecordInfo> getRecordList(int page, int count);
}
