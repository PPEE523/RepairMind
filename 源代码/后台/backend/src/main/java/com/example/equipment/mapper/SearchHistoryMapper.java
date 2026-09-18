package com.example.equipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.equipment.dto.SearchHistoryVO;
import com.example.equipment.entity.SearchHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SearchHistoryMapper extends BaseMapper<SearchHistory> {

    @Select("<script>" +
            "SELECT sh.*, su.username, su.nickname " +
            "FROM search_history sh " +
            "LEFT JOIN sys_user su ON sh.user_id = su.id " +
            "WHERE 1=1 " +
            "<if test='userId != null'> AND sh.user_id = #{userId} </if>" +
            "<if test='equipModel != null and equipModel != \"\"'> AND sh.equip_model = #{equipModel} </if>" +
            "<if test='searchSuccess != null'> AND sh.search_success = #{searchSuccess} </if>" +
            "<if test='startTime != null and startTime != \"\"'> AND sh.create_time &gt;= #{startTime} </if>" +
            "<if test='endTime != null and endTime != \"\"'> AND sh.create_time &lt;= #{endTime} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND sh.query_text LIKE CONCAT('%', #{keyword}, '%') </if>" +
            "ORDER BY sh.create_time DESC" +
            "</script>")
    Page<SearchHistoryVO> selectHistoryPage(Page<?> page,
                                             @Param("userId") Long userId,
                                             @Param("equipModel") String equipModel,
                                             @Param("searchSuccess") Boolean searchSuccess,
                                             @Param("startTime") String startTime,
                                             @Param("endTime") String endTime,
                                             @Param("keyword") String keyword);

    @Select("SELECT sh.*, su.username, su.nickname " +
            "FROM search_history sh " +
            "LEFT JOIN sys_user su ON sh.user_id = su.id " +
            "WHERE sh.id = #{id}")
    SearchHistoryVO selectHistoryDetail(@Param("id") Long id);
}
