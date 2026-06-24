package com.csg.prm.common.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.csg.prm.common.org.SysOrganization;
import org.apache.ibatis.annotations.Mapper;

/** 组织机构主数据只读 Mapper(平台/4A 同步表,PRM 不写)。 */
@Mapper
public interface SysOrganizationMapper extends BaseMapper<SysOrganization> {
}
