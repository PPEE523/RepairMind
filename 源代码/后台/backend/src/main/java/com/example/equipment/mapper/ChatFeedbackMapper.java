package com.example.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.equipment.entity.ChatFeedback;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI智能问答 - 反馈Mapper
 */
@Mapper
public interface ChatFeedbackMapper extends BaseMapper<ChatFeedback> {
}
