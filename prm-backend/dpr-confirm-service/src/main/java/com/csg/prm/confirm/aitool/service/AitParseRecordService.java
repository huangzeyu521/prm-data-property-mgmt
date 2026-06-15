package com.csg.prm.confirm.aitool.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.query.PageQuery;
import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.entity.AitParseRecord;
import com.csg.prm.confirm.aitool.entity.AitParseResult;
import com.csg.prm.confirm.aitool.mapper.AitParseRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 解析记录档服务(1.4#1):解析成功后逐字段留档,支持查询与 CSV 导出。
 */
@Service
public class AitParseRecordService {

    /** 留档的提取字段(标签 → 取值函数索引)。 */
    private static final String[] FIELDS = {
            "权利主体", "权利客体", "权利类型", "权利期限", "授权范围", "数据来源", "敏感类型", "印章真伪"};

    private final AitParseRecordMapper mapper;

    public AitParseRecordService(AitParseRecordMapper mapper) {
        this.mapper = mapper;
    }

    /** 解析成功后调用:把提取要素逐字段留档(时间/文档名/字段/值/置信度/操作人)。 */
    public void record(AitMaterial m, AitParseResult r, String operatorId, String operatorName) {
        if (m == null || r == null) {
            return;
        }
        String[] values = {
                r.getRightSubject(), r.getRightObject(), r.getRightType(), r.getRightTerm(),
                r.getAuthScope(), r.getDataSource(), r.getSensitiveType(), r.getSealValid()};
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < FIELDS.length; i++) {
            if (!StringUtils.hasText(values[i])) {
                continue;
            }
            AitParseRecord rec = new AitParseRecord();
            rec.setMaterialId(m.getMaterialId());
            rec.setFileName(m.getFileName());
            rec.setBatchNo(m.getBatchNo());
            rec.setField(FIELDS[i]);
            rec.setFieldValue(values[i]);
            rec.setConfidence(r.getConfidence());
            rec.setOperatorId(StringUtils.hasText(operatorId) ? operatorId : m.getCreatorId());
            rec.setOperatorName(operatorName);
            rec.setParseTime(now);
            mapper.insert(rec);
        }
    }

    public PageResult<AitParseRecord> page(PageQuery query, String fileName, String field, String operator) {
        LambdaQueryWrapper<AitParseRecord> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(fileName), AitParseRecord::getFileName, fileName)
                .eq(StringUtils.hasText(field), AitParseRecord::getField, field)
                .and(StringUtils.hasText(operator), q -> q.like(AitParseRecord::getOperatorName, operator)
                        .or().like(AitParseRecord::getOperatorId, operator))
                .orderByDesc(AitParseRecord::getParseTime);
        IPage<AitParseRecord> p = mapper.selectPage(query.toPage(), w);
        return PageResult.of(p);
    }

    /** 导出 CSV(UTF-8 BOM,Excel 友好)。 */
    public byte[] exportCsv(String fileName, String field, String operator) {
        LambdaQueryWrapper<AitParseRecord> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(fileName), AitParseRecord::getFileName, fileName)
                .eq(StringUtils.hasText(field), AitParseRecord::getField, field)
                .and(StringUtils.hasText(operator), q -> q.like(AitParseRecord::getOperatorName, operator)
                        .or().like(AitParseRecord::getOperatorId, operator))
                .orderByDesc(AitParseRecord::getParseTime);
        List<AitParseRecord> list = mapper.selectList(w);
        StringBuilder sb = new StringBuilder("﻿解析时间,文档名称,提取字段,提取值,置信度,操作人\n");
        for (AitParseRecord r : list) {
            sb.append(csv(r.getParseTime() == null ? "" : r.getParseTime().toString())).append(',')
                    .append(csv(r.getFileName())).append(',')
                    .append(csv(r.getField())).append(',')
                    .append(csv(r.getFieldValue())).append(',')
                    .append(r.getConfidence() == null ? "" : String.format("%.2f", r.getConfidence())).append(',')
                    .append(csv(StringUtils.hasText(r.getOperatorName()) ? r.getOperatorName() : r.getOperatorId()))
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String csv(String v) {
        if (v == null) {
            return "";
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
