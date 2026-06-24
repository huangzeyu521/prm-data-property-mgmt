package com.csg.prm.common.org;

import com.csg.prm.common.org.mapper.SysOrganizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 组织机构主数据本地种子(仅 dev/test):无真实平台/4A 同步源时,幂等播种南网"网→省→地市"层级,
 * 供部门/归口下拉、组织树与省地市码回填联调。生产由平台/4A 同步真表,PRM 只读不写,故本 Runner 不在 prod 生效。
 */
@Component
@Profile({"dev", "test"})
public class OrgSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrgSeedRunner.class);

    private final SysOrganizationMapper mapper;

    public OrgSeedRunner(SysOrganizationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long cnt = mapper.selectCount(null);
        if (cnt != null && cnt > 0) {
            return; // 幂等:已有数据(真实同步或已种)即跳过
        }
        // 网级
        ins("ORG-CSG", "中国南方电网有限责任公司", "CSG", "南方电网", null, "网级", "总部", null, "00");
        // 省级(五省网)
        ins("ORG-GD", "广东电网有限责任公司", "GD", "广东电网", "ORG-CSG", "省级", "省级单位", null, "10");
        ins("ORG-GX", "广西电网有限责任公司", "GX", "广西电网", "ORG-CSG", "省级", "省级单位", null, "20");
        ins("ORG-YN", "云南电网有限责任公司", "YN", "云南电网", "ORG-CSG", "省级", "省级单位", null, "30");
        ins("ORG-GZ", "贵州电网有限责任公司", "GZ", "贵州电网", "ORG-CSG", "省级", "省级单位", null, "40");
        ins("ORG-HI", "海南电网有限责任公司", "HI", "海南电网", "ORG-CSG", "省级", "省级单位", null, "50");
        // 地市供电局(以城市行政区划码作 bureauCode)
        ins("ORG-GD-GZ", "广州供电局", "4401", "广州局", "ORG-GD", "地市", "地市单位", "4401", "1001");
        ins("ORG-GD-SZ", "深圳供电局", "4403", "深圳局", "ORG-GD", "地市", "地市单位", "4403", "1002");
        ins("ORG-GD-FS", "佛山供电局", "4406", "佛山局", "ORG-GD", "地市", "地市单位", "4406", "1003");
        ins("ORG-GD-DG", "东莞供电局", "4419", "东莞局", "ORG-GD", "地市", "地市单位", "4419", "1004");
        ins("ORG-GX-NN", "南宁供电局", "4501", "南宁局", "ORG-GX", "地市", "地市单位", "4501", "2001");
        ins("ORG-YN-KM", "昆明供电局", "5301", "昆明局", "ORG-YN", "地市", "地市单位", "5301", "3001");
        ins("ORG-GZ-GY", "贵阳供电局", "5201", "贵阳局", "ORG-GZ", "地市", "地市单位", "5201", "4001");
        ins("ORG-HI-HK", "海口供电局", "4601", "海口局", "ORG-HI", "地市", "地市单位", "4601", "5001");
        log.info("[组织主数据] 本地种子完成(dev/test):南网 网→省→地市 {} 个组织", mapper.selectCount(null));
    }

    private void ins(String id, String name, String code, String shortName, String parentId,
                     String level, String type, String cityCode, String sortNo) {
        SysOrganization o = new SysOrganization();
        o.setId(id);
        o.setBizOrgId(id);
        o.setBizOrgName(name);
        o.setBizOrgCode(code);
        o.setShortName(shortName);
        o.setParentId(parentId);
        o.setOrgLevel(level);
        o.setOrgType(type);
        o.setCityCode(cityCode);
        o.setSortNo(sortNo);
        o.setBaseOrgCode(code);
        mapper.insert(o);
    }

    static List<String> provinceCodes() {
        return List.of("GD", "GX", "YN", "GZ", "HI");
    }
}
