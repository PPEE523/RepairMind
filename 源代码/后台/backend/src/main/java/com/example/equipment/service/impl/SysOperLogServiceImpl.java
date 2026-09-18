package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.SysOperLog;
import com.example.equipment.mapper.SysOperLogMapper;
import com.example.equipment.service.SysOperLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements SysOperLogService {

    @Override
    public void saveLog(Long userId, String operType, String content, String ipAddress) {
        SysOperLog log = new SysOperLog();
        log.setUserId(userId);
        log.setOperType(operType);
        log.setContent(content);
        log.setOperTime(LocalDateTime.now());
        log.setIpAddress(ipAddress);
        save(log);
    }
}
