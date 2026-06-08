package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResultCode;
import com.csg.prm.common.exception.BizException;
import com.csg.prm.confirm.entity.ConfirmGuidance;
import com.csg.prm.confirm.mapper.ConfirmGuidanceMapper;
import com.csg.prm.confirm.service.ConfirmGuidanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class ConfirmGuidanceServiceImpl implements ConfirmGuidanceService {

    private final ConfirmGuidanceMapper mapper;

    public ConfirmGuidanceServiceImpl(ConfirmGuidanceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String save(ConfirmGuidance g) {
        if (!StringUtils.hasText(g.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "标题不能为空");
        }
        // 带 ID:就地修改(不产生新版本)
        if (StringUtils.hasText(g.getGuidanceId())) {
            mapper.updateById(g);
            return g.getGuidanceId();
        }
        return newVersion(g);
    }

    @Override
    @Transactional
    public String uploadFile(ConfirmGuidance meta, String fileName, byte[] data) {
        if (meta == null || !StringUtils.hasText(meta.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "标题不能为空");
        }
        if (data == null || data.length == 0) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "上传文件为空");
        }
        meta.setFileName(fileName);
        meta.setFileData(Base64.getEncoder().encodeToString(data));
        return newVersion(meta);
    }

    /** 新建一个版本:版本号自增、同标题旧版本置非最新、本条置最新。 */
    private String newVersion(ConfirmGuidance g) {
        g.setGuidanceId(null);
        g.setVersion("v" + nextVersionNo(g.getTitle()));
        g.setIsLatest(Boolean.TRUE);
        if (g.getPublishDate() == null) {
            g.setPublishDate(LocalDateTime.now());
        }
        // 旧版本降级为非最新
        ConfirmGuidance demote = new ConfirmGuidance();
        demote.setIsLatest(Boolean.FALSE);
        mapper.update(demote, new LambdaQueryWrapper<ConfirmGuidance>()
                .eq(ConfirmGuidance::getTitle, g.getTitle()));
        mapper.insert(g);
        // 文件入库后 fileUrl 指向下载地址,便于前端展示/下载
        if (StringUtils.hasText(g.getFileName())) {
            ConfirmGuidance upd = new ConfirmGuidance();
            upd.setGuidanceId(g.getGuidanceId());
            upd.setFileUrl("/api/dpr/confirm/guidance/" + g.getGuidanceId() + "/download");
            mapper.updateById(upd);
        }
        return g.getGuidanceId();
    }

    private int nextVersionNo(String title) {
        List<ConfirmGuidance> same = mapper.selectList(new LambdaQueryWrapper<ConfirmGuidance>()
                .eq(ConfirmGuidance::getTitle, title)
                .select(ConfirmGuidance::getVersion));
        int max = 0;
        for (ConfirmGuidance c : same) {
            String v = c.getVersion();
            if (v != null && v.startsWith("v")) {
                try {
                    max = Math.max(max, Integer.parseInt(v.substring(1)));
                } catch (NumberFormatException ignore) {
                    // 非 vN 版本号忽略
                }
            }
        }
        return max + 1;
    }

    @Override
    public byte[] download(String guidanceId) {
        ConfirmGuidance g = getById(guidanceId);
        if (!StringUtils.hasText(g.getFileData())) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "该指引无可下载的原件(纯文本指引)");
        }
        return Base64.getDecoder().decode(g.getFileData());
    }

    @Override
    @Transactional
    public void delete(String guidanceId) {
        if (mapper.selectById(guidanceId) == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "指引材料不存在");
        }
        mapper.deleteById(guidanceId);
    }

    @Override
    public ConfirmGuidance getById(String guidanceId) {
        ConfirmGuidance g = mapper.selectById(guidanceId);
        if (g == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "指引材料不存在");
        }
        return g;
    }

    @Override
    public List<ConfirmGuidance> versions(String title) {
        List<ConfirmGuidance> list = mapper.selectList(new LambdaQueryWrapper<ConfirmGuidance>()
                .eq(StringUtils.hasText(title), ConfirmGuidance::getTitle, title)
                .orderByDesc(ConfirmGuidance::getCreateTime));
        list.forEach(g -> g.setFileData(null));
        return list;
    }

    @Override
    @Transactional
    public void setLatest(String guidanceId) {
        ConfirmGuidance g = getById(guidanceId);
        ConfirmGuidance demote = new ConfirmGuidance();
        demote.setIsLatest(Boolean.FALSE);
        mapper.update(demote, new LambdaQueryWrapper<ConfirmGuidance>()
                .eq(ConfirmGuidance::getTitle, g.getTitle()));
        ConfirmGuidance upd = new ConfirmGuidance();
        upd.setGuidanceId(guidanceId);
        upd.setIsLatest(Boolean.TRUE);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<ConfirmGuidance> page(long current, long size, String title, String guidanceType, boolean latestOnly) {
        LambdaQueryWrapper<ConfirmGuidance> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(title), ConfirmGuidance::getTitle, title)
                .eq(StringUtils.hasText(guidanceType), ConfirmGuidance::getGuidanceType, guidanceType)
                .eq(latestOnly, ConfirmGuidance::getIsLatest, Boolean.TRUE)
                .orderByDesc(ConfirmGuidance::getCreateTime);
        IPage<ConfirmGuidance> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        p.getRecords().forEach(g -> g.setFileData(null));
        return PageResult.of(p);
    }
}
