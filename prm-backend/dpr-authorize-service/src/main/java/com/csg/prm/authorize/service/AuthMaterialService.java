package com.csg.prm.authorize.service;

import com.csg.prm.authorize.entity.AuthMaterial;

import java.util.List;

/**
 * 授权申请材料服务(可研 3.2.2.1.1.3.1.4)。
 */
public interface AuthMaterialService {

    /** 真实文件上传:格式校验(PDF/Word/图片) + Base64 入库,返回 materialId。 */
    String uploadFile(AuthMaterial meta, String fileName, byte[] data);

    /** 下载/预览原件二进制。 */
    byte[] download(String materialId);

    AuthMaterial getById(String materialId);

    void delete(String materialId);

    /** 某申请的材料清单(置空 fileData)。 */
    List<AuthMaterial> listByApply(String applyId);
}
