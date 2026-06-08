package com.csg.prm.confirm.service;

import com.csg.prm.common.api.PageResult;
import com.csg.prm.confirm.entity.ConfirmGuidance;

import java.util.List;

/**
 * 确权指引材料服务(F-02-001-001-001)。
 * 支持上传/下载文件、历史版本记录(同标题保留全部版本,仅一条为最新)。
 */
public interface ConfirmGuidanceService {

    /** 保存:带 guidanceId=就地修改;否则按标题新建一个版本(版本号自增、置为最新)。 */
    String save(ConfirmGuidance g);

    /** 上传文件并作为新版本入库(Base64 存 fileData),返回 guidanceId。 */
    String uploadFile(ConfirmGuidance meta, String fileName, byte[] data);

    /** 下载某条指引的原件二进制(Base64 解码)。 */
    byte[] download(String guidanceId);

    void delete(String guidanceId);

    ConfirmGuidance getById(String guidanceId);

    /** 同标题的全部历史版本(按时间倒序)。 */
    List<ConfirmGuidance> versions(String title);

    /** 将某版本设为最新(同标题其余置为非最新)。 */
    void setLatest(String guidanceId);

    /** 分页;latestOnly=true 时仅返回每个标题的最新版本。 */
    PageResult<ConfirmGuidance> page(long current, long size, String title, String guidanceType, boolean latestOnly);
}
