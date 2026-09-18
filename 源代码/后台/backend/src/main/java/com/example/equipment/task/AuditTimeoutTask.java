package com.example.equipment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.equipment.entity.RepairKnowledge;
import com.example.equipment.service.RepairKnowledgeService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审核超时提醒任务
 */
@Slf4j
@Component
public class AuditTimeoutTask {

    @Resource
    private RepairKnowledgeService repairKnowledgeService;

    /**
     * 每小时检查一次待审核案例是否超过48小时
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkAuditTimeout() {
        log.info("开始检查审核超时案例...");

        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairKnowledge::getAuditStatus, 0)  // 待审核
                .lt(RepairKnowledge::getCreateTime, threshold);  // 超过48小时

        List<RepairKnowledge> timeoutList = repairKnowledgeService.list(wrapper);

        if (!timeoutList.isEmpty()) {
            log.warn("发现 {} 条待审核案例超过48小时", timeoutList.size());
            
            for (RepairKnowledge knowledge : timeoutList) {
                // TODO: 发送提醒通知（邮件、站内消息等）
                log.warn("案例超时提醒: id={}, title={}, uploadTime={}", 
                        knowledge.getId(), 
                        knowledge.getTitle(), 
                        knowledge.getCreateTime());
            }
        } else {
            log.info("没有超时的待审核案例");
        }
    }

    /**
     * 获取超时待审核案例数量
     */
    public int getTimeoutCount() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairKnowledge::getAuditStatus, 0)
                .lt(RepairKnowledge::getCreateTime, threshold);

        return (int) repairKnowledgeService.count(wrapper);
    }

    /**
     * 获取超时待审核案例列表
     */
    public List<RepairKnowledge> getTimeoutList() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(48);

        LambdaQueryWrapper<RepairKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepairKnowledge::getAuditStatus, 0)
                .lt(RepairKnowledge::getCreateTime, threshold)
                .orderByAsc(RepairKnowledge::getCreateTime);

        return repairKnowledgeService.list(wrapper);
    }
}
