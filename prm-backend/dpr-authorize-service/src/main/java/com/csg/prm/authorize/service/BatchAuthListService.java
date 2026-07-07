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

    /** 删除草案清单(仅草案态;级联删除其下草稿明细)。 */
    void delete(String batchListId);

    /** 撤回申报稿 -> 草案:清单回草案 + 其下在审明细退回草稿,便于再编辑/再提交(仅申报稿态)。 */
    void withdraw(String batchListId);
    /** 批准提交后,独立事务 best-effort 生成《运营授权协议》草案;返回协议ID(失败返回 null,不抛出)。 */
    String autoGenerateAgreementAfterApprove(String batchListId);
    BatchAuthList getById(String batchListId);
    PageResult<BatchAuthList> page(long current, long size, String listYear, String listStatus);
}
