package com.csg.prm.common.org;

import java.util.List;

/**
 * 组织机构主数据只读服务:提供组织树/清单与"归口网级"解析。
 * 数据源为平台/4A 同步的 SYS_ORGANIZATION,PRM 仅消费。
 */
public interface OrgService {

    /** 全量组织(按 sortNo/层级排序)。 */
    List<SysOrganization> listAll();

    /** 指定层级的组织(level 为空=全部);用于下拉(如只取"地市"局)。 */
    List<SysOrganization> listByLevel(String level);

    /** 组织树(根=网级,逐层 children)。 */
    List<OrgNode> tree();

    /**
     * 归口网级解析:按 组织名/缩写/bizOrgId/id 命中一个组织,沿 parentId 上溯,
     * 取省级节点 code 为 provinceCode、地市节点 cityCode/code 为 bureauCode。
     * 无法命中或为网级总部时返回 {@link Jurisdiction#EMPTY}。
     */
    Jurisdiction resolve(String orgKey);

    /**
     * 编码→名称反查:按已存的 province_code/bureau_code 命中组织,回填省/地市名称
     * (用于按编码维度统计的展示)。仅填能命中的名称,命中不到的名称留 null、码原样保留;
     * 两码皆空时返回 {@link Jurisdiction#EMPTY}。
     */
    Jurisdiction describe(String provinceCode, String bureauCode);

    /** 主数据变更后刷新缓存(平台同步后调用)。 */
    void reload();
}
