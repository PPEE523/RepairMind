package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.ChatHistory;
import com.example.equipment.mapper.ChatHistoryMapper;
import com.example.equipment.service.ChatHistoryService;
import org.springframework.stereotype.Service;

@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>
        implements ChatHistoryService {
}
