package com.fengzhi.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fengzhi.ai.model.dto.statistic.AppAnswerCountDTO;
import com.fengzhi.ai.model.dto.statistic.AppAnswerResultCountDTO;
import com.fengzhi.ai.model.entity.UserAnswer;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserAnswerMapper extends BaseMapper<UserAnswer> {

    @Select("select appId, count(userId) as answerCount from user_answer\n" +
            "    group by appId order by answerCount desc limit 10;")
    List<AppAnswerCountDTO> doAppAnswerCount();


    @Select("select resultName, count(resultName) as resultCount from user_answer\n" +
            "    where appId = #{appId}\n" +
            "    group by resultName order by resultCount desc;")
    List<AppAnswerResultCountDTO> doAppAnswerResultCount(Long appId);
}
