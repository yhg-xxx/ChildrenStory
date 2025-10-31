package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Story;

/**
 * 故事Mapper接口
 */
public interface StoryMapper extends BaseMapper<Story> {
    // 继承BaseMapper已包含常用的CRUD操作
    // 如需自定义查询，可在此添加方法并在对应的XML文件中实现
}