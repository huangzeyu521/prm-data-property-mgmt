package com.csg.prm.common.query;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.io.Serializable;

/**
 * 通用分页查询请求(对齐 data_pod 基准 cn.csg.datapod.domain.request.base.PageRequest)。
 * <p>入参契约(JSON/Query):{@code pageNum}(从 1 开始) / {@code pageSize}(1~100) / {@code keyword}(模糊查询)。
 * <p>另保留 {@link #getCurrent()}/{@link #getSize()}/{@link #toPage()} 仅作 MyBatis-Plus
 * {@code Page(current,size)} 对接的 Java 便捷别名(@JsonIgnore,不参与对外入参契约)。
 */
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码(从 1 开始) */
    @Min(value = 1, message = "页码最小为 1")
    private Integer pageNum = 1;

    /** 每页大小 */
    @Min(value = 1, message = "每页大小最小为 1")
    @Max(value = 100, message = "每页大小最大为 100")
    private Integer pageSize = 10;

    /** 关键词(模糊查询) */
    private String keyword;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /** MyBatis-Plus 习惯别名:当前页(等价 pageNum)。仅 Java 内部用,不入 JSON 契约。 */
    @JsonIgnore
    public long getCurrent() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    /**
     * MyBatis-Plus 习惯别名:每页大小(等价 pageSize)。仅 Java 内部用,不入 JSON 契约。
     * 不在此硬封顶——对外请求由 {@code @Max(100)} 校验拦截;服务内导出全量等场景可显式 setPageSize 取大值。
     */
    @JsonIgnore
    public long getSize() {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }

    /** 构建 MyBatis-Plus 分页对象。 */
    public <T> IPage<T> toPage() {
        return new Page<>(getCurrent(), getSize());
    }
}
