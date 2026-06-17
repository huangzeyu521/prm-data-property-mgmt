package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmApply;
import com.csg.prm.confirm.entity.ConfirmTableItem;
import com.csg.prm.confirm.mapper.ConfirmApplyMapper;
import com.csg.prm.confirm.mapper.ConfirmTableItemMapper;
import com.csg.prm.confirm.service.ConfirmConsolidationService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@Service
public class ConfirmConsolidationServiceImpl implements ConfirmConsolidationService {

    private static final String NW_COMPANY = "中国南方电网有限责任公司";
    private static final String REGULATED = "管制业务";
    private static final String REASON_BASE =
            "根据公司确权授权工作指引,在不违反法律法规合同约定前提下,确权时网公司直接合法取得分公司及全资子公司所有数据相应权益(无转让/转移动作);";

    /** 《数据确权信息汇总表》表头(32列,对齐官方模板) */
    private static final String[] CONFIRM_HEADERS = {
            "序号", "系统名称（匹配）", "系统归属单位", "实例名或TNS", "schema名称/模式名称", "表代码", "表名称", "表注释",
            "密级（不涉密/核心商密/普通商密/工作秘密/敏感信息）", "数据来源信息判定", "来源主体名称", "来源说明", "来源资料附件名称",
            "是否涉及行政监管要求", "G信息识别关联主体说明", "G信息识别关联附件名称",
            "是否涉及用户个人/家庭隐私", "H信息识别关联主体说明", "H信息识别关联附件名称",
            "是否涉及第三方商业机密", "I信息识别关联主体说明", "I信息识别关联附件名称",
            "是否存在其他数据权益约束的协议", "J信息识别关联主体说明", "J信息识别关联资料附件名称", "更新时间",
            "持有权", "持有权使用场景及目的摘要", "使用权", "使用权使用场景及目的摘要",
            "经营权（管制单位默认没有限经营权）", "经营权使用场景及目的摘要"};

    private final ConfirmTableItemMapper itemMapper;
    private final ConfirmApplyMapper applyMapper;

    public ConfirmConsolidationServiceImpl(ConfirmTableItemMapper itemMapper, ConfirmApplyMapper applyMapper) {
        this.itemMapper = itemMapper;
        this.applyMapper = applyMapper;
    }

    @Override
    @Transactional
    public int saveTableItems(String applyId, List<ConfirmTableItem> items) {
        requireApply(applyId);
        itemMapper.delete(new LambdaQueryWrapper<ConfirmTableItem>().eq(ConfirmTableItem::getApplyId, applyId));
        int saved = 0;
        for (ConfirmTableItem item : items) {
            if (!StringUtils.hasText(item.getTableCode()) && !StringUtils.hasText(item.getTableName())) {
                continue;
            }
            item.setItemId(null);
            item.setApplyId(applyId);
            itemMapper.insert(item);
            saved++;
        }
        return saved;
    }

    @Override
    public List<ConfirmTableItem> listTableItems(String applyId) {
        return itemMapper.selectList(new LambdaQueryWrapper<ConfirmTableItem>()
                .eq(ConfirmTableItem::getApplyId, applyId).orderByAsc(ConfirmTableItem::getCreateTime));
    }

    @Override
    public ConsolidationResult judgeConsolidation(String applyId) {
        ConfirmApply apply = requireApply(applyId);
        List<ConfirmTableItem> items = listTableItems(applyId);
        boolean regulated = REGULATED.equals(apply.getRegulated());
        boolean involvesThird = involvesThird(apply, items);
        boolean hasOperateClaim = apply.getRightType() != null && apply.getRightType().contains("经营");

        // 《权益内部管理汇总表》说明页 5 条权属判定规则(确权时直接判定网公司权益,无转让/转移动作)
        if (!involvesThird) {
            if (regulated) {
                return new ConsolidationResult("1.1", "有", "有", "有", false, apply.getRegulated(),
                        REASON_BASE + "自行生产且全部不涉及第三方,管制单位经营权调整为有,确权时直接归属网公司(不涉及第三方)。");
            }
            return new ConsolidationResult("1.2", "有", "有", hasOperateClaim ? "有" : "无", false, apply.getRegulated(),
                    REASON_BASE + "自行生产且全部不涉及第三方,非管制单位,相应权利确权时直接归属网公司(不涉及第三方)。");
        }
        if (!regulated) {
            if (hasOperateClaim) {
                return new ConsolidationResult("2.1", "有", "有", "依权益判定", true, apply.getRegulated(),
                        REASON_BASE + "涉及第三方权益,非管制单位且有经营权,经营权依权益判定确权时直接归属网公司(涉第三方须合规处置)。");
            }
            return new ConsolidationResult("2.2", "有", "有", "无", true, apply.getRegulated(),
                    REASON_BASE + "涉及第三方权益,非管制单位且无经营权,网公司无经营权。");
        }
        boolean otherRestriction = items.stream().anyMatch(i -> "是".equals(i.getIFlag()) || "是".equals(i.getJFlag()));
        return new ConsolidationResult("3.1", "有", "有", otherRestriction ? "无" : "依权益判定", true, apply.getRegulated(),
                REASON_BASE + "涉及第三方权益,管制单位先恢复经营权(去掉管制因素)再判定:"
                        + (otherRestriction ? "存在其他无经营权约束,网公司无经营权。" : "有经营权,确权时直接归属网公司。"));
    }

    @Override
    public byte[] exportConfirmSummary() {
        return exportWorkbook(false);
    }

    @Override
    public byte[] exportEquityConsolidation() {
        return exportWorkbook(true);
    }

    /** 双汇总表导出:equityMode=true 时输出 34 列权益内部管理表(前缀 权益主体/管制 + 后缀 共享判定原因) */
    private byte[] exportWorkbook(boolean equityMode) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(equityMode ? "数据权益内部管理汇总表" : "数据确权信息汇总表");
            Row header = sheet.createRow(0);
            int col = 0;
            if (equityMode) {
                header.createCell(col++).setCellValue("权益主体");
            }
            for (int i = equityMode ? 1 : 0; i < CONFIRM_HEADERS.length; i++) {
                header.createCell(col++).setCellValue(CONFIRM_HEADERS[i]);
                if (equityMode && i == 2) {
                    header.createCell(col++).setCellValue("管制/非管制");
                }
            }
            if (equityMode) {
                header.createCell(col).setCellValue("共享判定原因");
            }

            int rowIdx = 1;
            List<ConfirmApply> applies = applyMapper.selectList(null);
            for (ConfirmApply apply : applies) {
                List<ConfirmTableItem> items = listTableItems(apply.getApplyId());
                ConsolidationResult cr = equityMode ? judgeConsolidation(apply.getApplyId()) : null;
                for (ConfirmTableItem item : items) {
                    writeRow(sheet.createRow(rowIdx), rowIdx, apply, item, cr, equityMode);
                    rowIdx++;
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("汇总表导出失败", e);
        }
    }

    private void writeRow(Row row, int seq, ConfirmApply apply, ConfirmTableItem it,
                          ConsolidationResult cr, boolean equityMode) {
        String rt = apply.getRightType() == null ? "" : apply.getRightType();
        String scene = nv(it.getTableName()) + "来源于系统「" + nv(apply.getAssetName()) + "」,按数据来源与主体关联权益判定";
        int c = 0;
        if (equityMode) {
            // 权益表无"序号"列:首列为权益主体(网公司)
            row.createCell(c++).setCellValue(NW_COMPANY);
        } else {
            row.createCell(c++).setCellValue(seq);
        }
        row.createCell(c++).setCellValue(nv(apply.getAssetName()));
        row.createCell(c++).setCellValue(nv(apply.getRightHolder()));
        if (equityMode) {
            row.createCell(c++).setCellValue(nv(apply.getRegulated()));
        }
        row.createCell(c++).setCellValue(nv(it.getInstanceName()));
        row.createCell(c++).setCellValue(nv(it.getSchemaName()));
        row.createCell(c++).setCellValue(nv(it.getTableCode()));
        row.createCell(c++).setCellValue(nv(it.getTableName()));
        row.createCell(c++).setCellValue(nv(it.getTableComment()));
        row.createCell(c++).setCellValue(nv(it.getSecretLevel()));
        row.createCell(c++).setCellValue(nv(it.getSourceType()));
        row.createCell(c++).setCellValue(nv(it.getSourceSubject()));
        row.createCell(c++).setCellValue(nv(it.getSourceDesc()));
        row.createCell(c++).setCellValue(attachName(apply, it.getSourceType(), "B"));
        c = ghijCells(row, c, apply, it.getGFlag(), it.getGSubject(), "G");
        c = ghijCells(row, c, apply, it.getHFlag(), it.getHSubject(), "H");
        c = ghijCells(row, c, apply, it.getIFlag(), it.getISubject(), "I");
        c = ghijCells(row, c, apply, it.getJFlag(), it.getJSubject(), "J");
        row.createCell(c++).setCellValue(it.getUpdateTime() == null ? "" : it.getUpdateTime().toLocalDate().toString());
        if (equityMode) {
            row.createCell(c++).setCellValue(cr.holdRight());
            row.createCell(c++).setCellValue(scene);
            row.createCell(c++).setCellValue(cr.useRight());
            row.createCell(c++).setCellValue(scene);
            row.createCell(c++).setCellValue(cr.operateRight());
            row.createCell(c++).setCellValue(scene);
            row.createCell(c).setCellValue(cr.reason());
        } else {
            row.createCell(c++).setCellValue(rt.contains("持有") ? "有" : "无");
            row.createCell(c++).setCellValue(scene);
            row.createCell(c++).setCellValue(rt.contains("使用") || rt.contains("加工") ? "有" : "无");
            row.createCell(c++).setCellValue(scene);
            row.createCell(c++).setCellValue(rt.contains("经营") && !REGULATED.equals(apply.getRegulated()) ? "有" : "无");
            row.createCell(c).setCellValue(scene);
        }
    }

    /** G/H/I/J 三列:是否 + 主体说明 + 附件名(命名规范:系统名称+类型+序号) */
    private int ghijCells(Row row, int c, ConfirmApply apply, String flag, String subject, String type) {
        boolean yes = "是".equals(flag);
        row.createCell(c++).setCellValue(yes ? "是" : "否");
        row.createCell(c++).setCellValue(yes ? nv(subject) : "");
        row.createCell(c++).setCellValue(yes ? nv(apply.getAssetName()) + type + "01" : "");
        return c;
    }

    /** 来源资料附件名:非 A 自行生产时按 系统名+来源类型字母+序号 给出建议名 */
    private String attachName(ConfirmApply apply, String sourceType, String fallback) {
        if (sourceType == null || sourceType.startsWith("A")) {
            return "";
        }
        String letter = sourceType.isEmpty() ? fallback : sourceType.substring(0, 1);
        return nv(apply.getAssetName()) + letter + "01";
    }

    /** 涉第三方判定:来源判定非 A(B-F) 或 G/H/I/J 任一为是 或 申请已标记涉第三方 */
    private boolean involvesThird(ConfirmApply apply, List<ConfirmTableItem> items) {
        if (Boolean.TRUE.equals(apply.getInvolvesThirdParty())) {
            return true;
        }
        return items.stream().anyMatch(i ->
                (StringUtils.hasText(i.getSourceType()) && !i.getSourceType().startsWith("A"))
                        || "是".equals(i.getGFlag()) || "是".equals(i.getHFlag())
                        || "是".equals(i.getIFlag()) || "是".equals(i.getJFlag()));
    }

    private ConfirmApply requireApply(String applyId) {
        ConfirmApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "确权申请不存在");
        }
        return apply;
    }

    private String nv(String s) {
        return s == null ? "" : s;
    }
}
