package com.csg.prm.authorize.service;

import com.csg.prm.authorize.dto.AuthApplyQuery;
import com.csg.prm.authorize.entity.AuthApply;
import com.csg.prm.common.api.PageResult;

import java.util.List;

/**
 * 数据授权申请与审批服务。
 * 强制"先确后授":提交时校验引用的权益卡片有效;终审通过自动生成授权证书。
 */
public interface AuthApplyService {

    String saveDraft(AuthApply apply);

    /** 按批量清单ID查其下授权项(表6 明细行) */
    List<AuthApply> byBatch(String batchListId);

    /** 提交审批(校验先确后授;草稿 -> 审核中) */
    void submit(String applyId);

    /** 审批通过 -> 已生效,自动生成授权证书,返回证书ID */
    String approve(String applyId);

    void reject(String applyId, String reason);

    AuthApply getById(String applyId);

    PageResult<AuthApply> page(AuthApplyQuery query);
}
