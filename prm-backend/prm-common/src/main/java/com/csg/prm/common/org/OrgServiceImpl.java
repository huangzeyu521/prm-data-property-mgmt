package com.csg.prm.common.org;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.org.mapper.SysOrganizationMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrgServiceImpl implements OrgService {

    static final String LEVEL_PROVINCE = "省级";
    static final String LEVEL_BUREAU = "地市";
    private static final int MAX_DEPTH = 16; // 上溯防御:杜绝脏数据成环导致死循环

    private final SysOrganizationMapper mapper;

    /** 主数据缓存(低频变更):id->组织 + 全量列表;volatile 双引用,reload 原子替换。 */
    private volatile Map<String, SysOrganization> byId;
    private volatile List<SysOrganization> all;

    public OrgServiceImpl(SysOrganizationMapper mapper) {
        this.mapper = mapper;
    }

    private void ensureLoaded() {
        if (byId != null) {
            return;
        }
        synchronized (this) {
            if (byId == null) {
                loadInternal();
            }
        }
    }

    private void loadInternal() {
        List<SysOrganization> list = mapper.selectList(new LambdaQueryWrapper<SysOrganization>()
                .orderByAsc(SysOrganization::getSortNo));
        Map<String, SysOrganization> map = new LinkedHashMap<>();
        for (SysOrganization o : list) {
            if (o.getId() != null) {
                map.put(o.getId(), o);
            }
        }
        this.all = list;
        this.byId = map;
    }

    @Override
    public synchronized void reload() {
        loadInternal();
    }

    @Override
    public List<SysOrganization> listAll() {
        ensureLoaded();
        return new ArrayList<>(all);
    }

    @Override
    public List<SysOrganization> listByLevel(String level) {
        ensureLoaded();
        if (!StringUtils.hasText(level)) {
            return new ArrayList<>(all);
        }
        List<SysOrganization> r = new ArrayList<>();
        for (SysOrganization o : all) {
            if (level.equals(o.getOrgLevel())) {
                r.add(o);
            }
        }
        return r;
    }

    @Override
    public List<OrgNode> tree() {
        ensureLoaded();
        Map<String, OrgNode> nodes = new LinkedHashMap<>();
        for (SysOrganization o : all) {
            nodes.put(o.getId(), OrgNode.of(o));
        }
        List<OrgNode> roots = new ArrayList<>();
        for (SysOrganization o : all) {
            OrgNode n = nodes.get(o.getId());
            OrgNode parent = o.getParentId() == null ? null : nodes.get(o.getParentId());
            if (parent != null) {
                parent.getChildren().add(n);
            } else {
                roots.add(n);
            }
        }
        sortRec(roots);
        return roots;
    }

    private void sortRec(List<OrgNode> ns) {
        ns.sort(Comparator.comparing(n -> n.getSortNo() == null ? "" : n.getSortNo()));
        for (OrgNode n : ns) {
            sortRec(n.getChildren());
        }
    }

    @Override
    public Jurisdiction resolve(String orgKey) {
        if (!StringUtils.hasText(orgKey)) {
            return Jurisdiction.EMPTY;
        }
        ensureLoaded();
        SysOrganization hit = findByKey(orgKey.trim());
        if (hit == null) {
            return Jurisdiction.EMPTY;
        }
        String provinceCode = null, provinceName = null, bureauCode = null, bureauName = null;
        SysOrganization cur = hit;
        int depth = 0;
        while (cur != null && depth++ < MAX_DEPTH) {
            if (LEVEL_PROVINCE.equals(cur.getOrgLevel()) && provinceCode == null) {
                provinceCode = cur.getBizOrgCode();
                provinceName = cur.getBizOrgName();
            }
            if (LEVEL_BUREAU.equals(cur.getOrgLevel()) && bureauCode == null) {
                bureauCode = StringUtils.hasText(cur.getCityCode()) ? cur.getCityCode() : cur.getBizOrgCode();
                bureauName = cur.getBizOrgName();
            }
            cur = cur.getParentId() == null ? null : byId.get(cur.getParentId());
        }
        if (provinceCode == null && bureauCode == null) {
            return Jurisdiction.EMPTY;
        }
        return new Jurisdiction(provinceCode, provinceName, bureauCode, bureauName);
    }

    @Override
    public Jurisdiction describe(String provinceCode, String bureauCode) {
        boolean hasP = StringUtils.hasText(provinceCode);
        boolean hasB = StringUtils.hasText(bureauCode);
        if (!hasP && !hasB) {
            return Jurisdiction.EMPTY;
        }
        ensureLoaded();
        String provinceName = null, bureauName = null;
        for (SysOrganization o : all) {
            if (hasP && provinceName == null
                    && LEVEL_PROVINCE.equals(o.getOrgLevel()) && provinceCode.equals(o.getBizOrgCode())) {
                provinceName = o.getBizOrgName();
            }
            if (hasB && bureauName == null && LEVEL_BUREAU.equals(o.getOrgLevel())
                    && (bureauCode.equals(o.getCityCode()) || bureauCode.equals(o.getBizOrgCode()))) {
                bureauName = o.getBizOrgName();
            }
        }
        return new Jurisdiction(hasP ? provinceCode : null, provinceName,
                hasB ? bureauCode : null, bureauName);
    }

    /** 命中优先级:bizOrgId/id 精确 > 组织名精确 > 缩写精确 > 组织名包含。 */
    private SysOrganization findByKey(String key) {
        SysOrganization byName = null, byShort = null, byContains = null;
        for (SysOrganization o : all) {
            if (key.equals(o.getBizOrgId()) || key.equals(o.getId())) {
                return o;
            }
            if (byName == null && key.equals(o.getBizOrgName())) {
                byName = o;
            }
            if (byShort == null && key.equals(o.getShortName())) {
                byShort = o;
            }
            if (byContains == null && o.getBizOrgName() != null
                    && (o.getBizOrgName().contains(key) || key.contains(o.getBizOrgName()))) {
                byContains = o;
            }
        }
        if (byName != null) {
            return byName;
        }
        if (byShort != null) {
            return byShort;
        }
        return byContains;
    }
}
