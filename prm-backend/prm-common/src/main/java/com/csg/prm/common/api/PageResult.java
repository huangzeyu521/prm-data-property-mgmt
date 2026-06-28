package com.csg.prm.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页查询响应包装类(对齐 data_pod / cn.csg.datapod 规范)。
 * <p>作为 {@link Result} 的 data 字段返回,信封字段 {@code {total, pageNum, pageSize, records}}。
 *
 * @param <T> 记录元素类型
 */
// 注:泛型类不加 @Schema(name=...),否则 springdoc 收敛泛型、PageResult<T> 的元素类型丢失。仅字段级标注。
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    @Schema(description = "总记录数", example = "100")
    private Long total;
    /** 当前页码(从 1 开始) */
    @Schema(description = "当前页码(从 1 开始)", example = "1")
    private Integer pageNum;
    /** 每页大小 */
    @Schema(description = "每页大小", example = "10")
    private Integer pageSize;
    /** 当前页数据列表 */
    @Schema(description = "当前页数据列表")
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = records;
    }

    /** 由 MyBatis-Plus IPage 适配为统一分页结果。 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), (int) page.getCurrent(), (int) page.getSize(), page.getRecords());
    }

    /** 全参快速构建。 */
    public static <T> PageResult<T> of(Long total, Integer pageNum, Integer pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    /** 空分页结果(默认第 1 页、每页 10)。 */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, 1, 10, Collections.emptyList());
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

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

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
