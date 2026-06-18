package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.BatchComplianceResult;
import com.csg.prm.authorize.entity.BatchAuthList;
import com.csg.prm.common.api.PageResult;

/**
 * 表6《数据批量授权清单》服务。草案 -> 申报稿 -> 批准。
 */
public interface BatchAuthListService {
    String create(BatchAuthList list);
    /** 草案 -> 申报稿 */
    void submit(String batchListId);
    /** 只读合规试跑:逐项校验(与 submit 门禁同源),返回整单是否可提交+被拦原因。不改状态。 */
    BatchComplianceResult complianceCheck(String batchListId);
    /** 申报稿 -> 批准(领导小组办公室) */
    void approve(String batchListId);
    BatchAuthList getById(String batchListId);
    PageResult<BatchAuthList> page(long current, long size, String listYear, String listStatus);
}
