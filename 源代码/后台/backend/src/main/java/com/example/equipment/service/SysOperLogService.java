package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.entity.SysOperLog;

public interface SysOperLogService extends IService<SysOperLog> {

    void saveLog(Long userId, String operType, String content, String ipAddress);
}
