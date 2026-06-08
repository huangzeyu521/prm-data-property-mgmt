package com.csg.prm.ledger.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.prm.ledger.entity.PropertyArchive;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产权档案 Mapper。继承 MyBatis-Plus BaseMapper 获得通用 CRUD。
 */
@Mapper
public interface PropertyArchiveMapper extends BaseMapper<PropertyArchive> {
}
