package com.example.equipment.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.equipment.dto.SearchHistoryQuery;
import com.example.equipment.dto.SearchHistoryVO;
import com.example.equipment.entity.SearchHistory;

import java.util.List;

public interface SearchHistoryService extends IService<SearchHistory> {

    Page<SearchHistoryVO> getHistoryList(SearchHistoryQuery query);

    Page<SearchHistoryVO> getUserHistoryList(Long userId, SearchHistoryQuery query);

    Page<SearchHistory> getUserHistory(Long userId, Integer pageNum, Integer pageSize);

    SearchHistoryVO getHistoryDetail(Long id);

    void batchDeleteHistory(Long userId, List<Long> ids);
}
