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
    private final OrgService orgService;

    public OrgSeedRunner(SysOrganizationMapper mapper, OrgService orgService) {
        this.mapper = mapper;
        this.orgService = orgService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Long cnt = mapper.selectCount(null);
        if (cnt != null && cnt > 0) {
            return; // 幂等:已有数据(真实同步或已种)即跳过
        }
        // 网级
        ins("ORG-CSG", "中国南方电网有限责任公司", "CSG", "南方电网", null, "网级", "总部", null, "00");
        // 网级直属单位(系统部署单位"打√"口径:与五省网并列单列)
        ins("ORG-EHV", "南方电网超高压输电公司", "EHV", "超高压", "ORG-CSG", "网级", "直属单位", null, "01");
        ins("ORG-DISP", "南方电网总调(双调中心)", "DISP", "双调", "ORG-CSG", "网级", "直属单位", null, "02");
        // 网级数据经营主体 / 分子公司(可作授权"被授权方":35号文 para51 网级经营主体;orgType=经营主体 供被授权方过滤)
        ins("ORG-DIGITAL", "南方电网数字集团有限公司", "DIGITAL", "数字集团", "ORG-CSG", "网级", "经营主体", null, "03");
        ins("ORG-ENERGY", "南网综合能源股份有限公司", "ENERGY", "综合能源", "ORG-CSG", "网级", "经营主体", null, "04");
        ins("ORG-PEAK", "南方电网调峰调频发电有限公司", "PEAK", "调峰调频", "ORG-CSG", "网级", "经营主体", null, "05");
        ins("ORG-CSRI", "南方电网科学研究院有限责任公司", "CSRI", "南网科研院", "ORG-CSG", "网级", "经营主体", null, "06");
        ins("ORG-PARTNER", "南方电网能源发展研究院有限责任公司", "ENDRI", "能源院", "ORG-CSG", "网级", "经营主体", null, "07");
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
        // 关键:刷新组织服务缓存。OrgServiceImpl 懒加载缓存一次且不自刷新,若启动期有组件先于本 Runner
        // 触发了 ensureLoaded(此时表空),缓存会永久为空 → /org/list 返回 []。播种后强制 reload 兜底,与运行顺序无关。
        orgService.reload();
        log.info("[组织主数据] 本地种子完成(dev/test):南网 网→省→地市 {} 个组织(已刷新缓存)", mapper.selectCount(null));
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
