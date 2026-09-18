package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.entity.AiFeedback;
import com.example.equipment.mapper.AiFeedbackMapper;
import com.example.equipment.service.AiFeedbackService;
import org.springframework.stereotype.Service;

@Service
public class AiFeedbackServiceImpl extends ServiceImpl<AiFeedbackMapper, AiFeedback>
        implements AiFeedbackService {
}
