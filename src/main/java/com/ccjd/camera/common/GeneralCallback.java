package com.ccjd.camera.common;

public interface GeneralCallback<T>{
    void run(int code, String msg, T data);
}
