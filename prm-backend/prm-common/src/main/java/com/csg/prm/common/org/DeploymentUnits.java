package com.csg.prm.common.org;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 系统部署单位(南网"打√"口径,附录 系统部署单位清单):固定 10 个部署单位的分类器。
 * <p>区别于 {@link Jurisdiction} 的"省→地市"层级:此处为<b>扁平</b>枚举——广州/深圳作为副省级供电局与五省网并列单列,
 * 超高压、双调为网级直属单位。统计据资产现有 province_code / bureau_code / 子公司(或系统)名按"最具体优先"归类,
 * 无法归类落「未标识」(随平台编码回填自然消除)。不新增库列,纯派生。
 */
public final class DeploymentUnits {

    public static final String HQ = "总部";
    public static final String EHV = "超高压";
    public static final String DISPATCH = "双调";
    public static final String GD = "广东";
    public static final String GX = "广西";
    public static final String YN = "云南";
    public static final String GZ = "贵州";
    public static final String HI = "海南";
    public static final String GUANGZHOU = "广州";
    public static final String SHENZHEN = "深圳";
    public static final String UNIDENTIFIED = "未标识";

    /** 固定展示顺序(界面"打√"清单顺序);统计时按此序零填充,确保 10 个单位恒显。 */
    public static final List<String> ORDER = List.of(
            HQ, EHV, DISPATCH, GD, GX, YN, GZ, HI, GUANGZHOU, SHENZHEN);

    private DeploymentUnits() {
    }

    /**
     * 据资产归属归类到部署单位。优先级:具体地市(广州/深圳)→ 网级直属(超高压/双调/总部)→ 五省网 → 未标识。
     *
     * @param provinceCode 省码(GD/GX/YN/GZ/HI),可空
     * @param bureauCode   地市码(行政区划码,如 4401 广州/4403 深圳),可空
     * @param name         归属名(子公司名/系统部署单位名/权属主体),可空
     */
    public static String classify(String provinceCode, String bureauCode, String name) {
        String n = name == null ? "" : name;
        // 1. 副省级供电局单列(地市码优先,再退归属名)——必须先于省网,否则被并入广东
        if ("4401".equals(bureauCode) || n.contains("广州")) {
            return GUANGZHOU;
        }
        if ("4403".equals(bureauCode) || n.contains("深圳")) {
            return SHENZHEN;
        }
        // 2. 网级直属单位
        if (n.contains("超高压")) {
            return EHV;
        }
        if (n.contains("双调") || n.contains("总调") || n.contains("调度控制") || n.contains("调控")) {
            return DISPATCH;
        }
        if (n.contains("总部") || n.contains("南方电网总") || "CSG".equalsIgnoreCase(provinceCode)
                || n.equals("中国南方电网有限责任公司") || (n.contains("南方电网") && !n.contains("电网有限责任公司分"))) {
            // 仅"南方电网"本部归总部;省网名("广东电网有限责任公司")不含"南方电网",不会误命中
            return HQ;
        }
        // 3. 五省网(省码优先,再退归属名)
        String byProvince = provinceBucket(provinceCode, n);
        if (byProvince != null) {
            return byProvince;
        }
        return UNIDENTIFIED;
    }

    private static String provinceBucket(String provinceCode, String n) {
        if ("GD".equalsIgnoreCase(provinceCode) || n.contains("广东")) {
            return GD;
        }
        if ("GX".equalsIgnoreCase(provinceCode) || n.contains("广西")) {
            return GX;
        }
        if ("YN".equalsIgnoreCase(provinceCode) || n.contains("云南")) {
            return YN;
        }
        if ("GZ".equalsIgnoreCase(provinceCode) || n.contains("贵州")) {
            return GZ;
        }
        if ("HI".equalsIgnoreCase(provinceCode) || n.contains("海南")) {
            return HI;
        }
        return null;
    }

    /** 是否为可识别归属(用于判断 name 是否提供了有效线索)。 */
    public static boolean isResolvable(String provinceCode, String bureauCode, String name) {
        return StringUtils.hasText(provinceCode) || StringUtils.hasText(bureauCode) || StringUtils.hasText(name);
    }
}
