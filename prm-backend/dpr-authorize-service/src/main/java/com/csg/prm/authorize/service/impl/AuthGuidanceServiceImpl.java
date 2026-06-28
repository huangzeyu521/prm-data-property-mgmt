package com.csg.prm.authorize.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csg.prm.authorize.entity.AuthGuidance;
import com.csg.prm.authorize.mapper.AuthGuidanceMapper;
import com.csg.prm.authorize.service.AuthGuidanceService;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AuthGuidanceServiceImpl implements AuthGuidanceService {

    private final AuthGuidanceMapper mapper;

    public AuthGuidanceServiceImpl(AuthGuidanceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public String save(AuthGuidance g) {
        if (!StringUtils.hasText(g.getTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "标题不能为空");
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
    public String uploadFile(AuthGuidance meta, String fileName, byte[] data) {
        if (meta == null || !StringUtils.hasText(meta.getTitle())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "标题不能为空");
        }
        if (data == null || data.length == 0) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "上传文件为空");
        }
        meta.setFileName(fileName);
        meta.setFileData(Base64.getEncoder().encodeToString(data));
        return newVersion(meta);
    }

    /** 新建一个版本:版本号自增、同标题旧版本置非最新、本条置最新。 */
    private String newVersion(AuthGuidance g) {
        g.setGuidanceId(null);
        g.setVersion("v" + nextVersionNo(g.getTitle()));
        g.setIsLatest(Boolean.TRUE);
        if (g.getPublishDate() == null) {
            g.setPublishDate(LocalDateTime.now());
        }
        // 旧版本降级为非最新
        AuthGuidance demote = new AuthGuidance();
        demote.setIsLatest(Boolean.FALSE);
        mapper.update(demote, new LambdaQueryWrapper<AuthGuidance>()
                .eq(AuthGuidance::getTitle, g.getTitle()));
        mapper.insert(g);
        // 文件入库后 fileUrl 指向下载地址,便于前端展示/下载
        if (StringUtils.hasText(g.getFileName())) {
            AuthGuidance upd = new AuthGuidance();
            upd.setGuidanceId(g.getGuidanceId());
            upd.setFileUrl("/api/dpr/auth/guidance/" + g.getGuidanceId() + "/download");
            mapper.updateById(upd);
        }
        return g.getGuidanceId();
    }

    private int nextVersionNo(String title) {
        List<AuthGuidance> same = mapper.selectList(new LambdaQueryWrapper<AuthGuidance>()
                .eq(AuthGuidance::getTitle, title)
                .select(AuthGuidance::getVersion));
        int max = 0;
        for (AuthGuidance c : same) {
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
        AuthGuidance g = getById(guidanceId);
        if (!StringUtils.hasText(g.getFileData())) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "该指引无可下载的原件(纯文本指引)");
        }
        return Base64.getDecoder().decode(g.getFileData());
    }

    @Override
    @Transactional
    public void delete(String guidanceId) {
        if (mapper.selectById(guidanceId) == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "指引材料不存在");
        }
        mapper.deleteById(guidanceId);
    }

    @Override
    public AuthGuidance getById(String guidanceId) {
        AuthGuidance g = mapper.selectById(guidanceId);
        if (g == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "指引材料不存在");
        }
        return g;
    }

    @Override
    public List<AuthGuidance> versions(String title) {
        List<AuthGuidance> list = mapper.selectList(new LambdaQueryWrapper<AuthGuidance>()
                .eq(StringUtils.hasText(title), AuthGuidance::getTitle, title)
                .orderByDesc(AuthGuidance::getCreateTime));
        list.forEach(g -> g.setFileData(null));
        return list;
    }

    @Override
    @Transactional
    public void setLatest(String guidanceId) {
        AuthGuidance g = getById(guidanceId);
        AuthGuidance demote = new AuthGuidance();
        demote.setIsLatest(Boolean.FALSE);
        mapper.update(demote, new LambdaQueryWrapper<AuthGuidance>()
                .eq(AuthGuidance::getTitle, g.getTitle()));
        AuthGuidance upd = new AuthGuidance();
        upd.setGuidanceId(guidanceId);
        upd.setIsLatest(Boolean.TRUE);
        mapper.updateById(upd);
    }

    @Override
    public PageResult<AuthGuidance> page(long current, long size, String title, String guidanceType, boolean latestOnly) {
        LambdaQueryWrapper<AuthGuidance> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(title), AuthGuidance::getTitle, title)
                .eq(StringUtils.hasText(guidanceType), AuthGuidance::getGuidanceType, guidanceType)
                .eq(latestOnly, AuthGuidance::getIsLatest, Boolean.TRUE)
                .orderByDesc(AuthGuidance::getCreateTime);
        IPage<AuthGuidance> p = mapper.selectPage(new Page<>(current <= 0 ? 1 : current, size <= 0 ? 10 : size), w);
        p.getRecords().forEach(g -> g.setFileData(null));
        return PageResult.of(p);
    }
}
