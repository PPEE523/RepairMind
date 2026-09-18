package com.example.equipment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.equipment.dto.SearchHistoryQuery;
import com.example.equipment.dto.SearchHistoryVO;
import com.example.equipment.entity.SearchHistory;
import com.example.equipment.mapper.SearchHistoryMapper;
import com.example.equipment.service.SearchHistoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchHistoryServiceImpl extends ServiceImpl<SearchHistoryMapper, SearchHistory>
        implements SearchHistoryService {

    @Override
    public Page<SearchHistoryVO> getHistoryList(SearchHistoryQuery query) {
        Page<SearchHistoryVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectHistoryPage(page, null, query.getEquipModel(),
                query.getSearchSuccess(), query.getStartTime(), query.getEndTime(), query.getKeyword());
    }

    @Override
    public Page<SearchHistoryVO> getUserHistoryList(Long userId, SearchHistoryQuery query) {
        Page<SearchHistoryVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return baseMapper.selectHistoryPage(page, userId, query.getEquipModel(),
                query.getSearchSuccess(), query.getStartTime(), query.getEndTime(), query.getKeyword());
    }

    @Override
    public Page<SearchHistory> getUserHistory(Long userId, Integer pageNum, Integer pageSize) {
        Page<SearchHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SearchHistory::getUserId, userId)
                .orderByDesc(SearchHistory::getCreateTime);
        return page(page, wrapper);
    }

    @Override
    public SearchHistoryVO getHistoryDetail(Long id) {
        return baseMapper.selectHistoryDetail(id);
    }

    @Override
    public void batchDeleteHistory(Long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SearchHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SearchHistory::getId, ids)
                .eq(SearchHistory::getUserId, userId);
        remove(wrapper);
    }
}
