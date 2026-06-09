package com.csg.prm.authorize.controller;

import com.csg.prm.authorize.entity.AuthMaterial;
import com.csg.prm.authorize.service.AuthMaterialService;
import com.csg.prm.common.api.R;
import com.csg.prm.common.exception.BizException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 授权申请材料接口(可研 3.2.2.1.1.3.1.4):上传/下载/查询/删除。 */
@RestController
@RequestMapping("/api/dpr/auth/material")
public class AuthMaterialController {

    private final AuthMaterialService service;

    public AuthMaterialController(AuthMaterialService service) {
        this.service = service;
    }

    /** 上传申请材料(multipart:file + applyId/材料名/类型/上传人)。 */
    @PostMapping("/upload-file")
    public R<String> uploadFile(@RequestParam("file") MultipartFile file,
                                @RequestParam String applyId,
                                @RequestParam(required = false) String materialName,
                                @RequestParam(required = false) String materialType,
                                @RequestParam(required = false) String owner) {
        if (file == null || file.isEmpty()) {
            throw new BizException("未选择文件");
        }
        AuthMaterial meta = new AuthMaterial();
        meta.setApplyId(applyId);
        meta.setMaterialName(materialName);
        meta.setMaterialType(materialType);
        meta.setOwner(owner);
        try {
            return R.ok(service.uploadFile(meta, file.getOriginalFilename(), file.getBytes()));
        } catch (IOException e) {
            throw new BizException("读取上传文件失败:" + e.getMessage());
        }
    }

    /** 在线预览/下载原件。 */
    @GetMapping("/{materialId}/file")
    public ResponseEntity<byte[]> file(@PathVariable String materialId) {
        byte[] data = service.download(materialId);
        String name = service.getById(materialId).getFileName();
        return ResponseEntity.ok()
                .header("Content-Disposition", ContentDisposition.inline()
                        .filename(name == null ? "material" : name, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{materialId}")
    public R<Void> delete(@PathVariable String materialId) {
        service.delete(materialId);
        return R.ok();
    }

    @GetMapping("/by-apply/{applyId}")
    public R<List<AuthMaterial>> byApply(@PathVariable String applyId) {
        return R.ok(service.listByApply(applyId));
    }
}
