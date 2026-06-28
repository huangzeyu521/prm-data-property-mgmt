package com.csg.prm.confirm.aitool.storage;

import com.csg.prm.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件系统存储(默认实现):写入 ${prm.storage.dir}/ait,返回相对路径(ait/<uuid>.<ext>)。
 * 生产以 OBS/MinIO 网关 @Primary 覆盖。
 */
@Component
public class LocalFileStorageGateway implements FileStorageGateway {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageGateway.class);
    private static final String SUBDIR = "ait";

    private final Path baseDir;

    public LocalFileStorageGateway(@Value("${prm.storage.dir:/app/_ait_storage}") String dir) {
        this.baseDir = Paths.get(dir);
        log.info("[确权材料存储] 本地存储目录={}", this.baseDir.toAbsolutePath());
    }

    @Override
    public String save(String fileName, byte[] data) {
        try {
            Path dir = baseDir.resolve(SUBDIR);
            Files.createDirectories(dir);
            String ext = ext(fileName);
            String key = SUBDIR + "/" + UUID.randomUUID().toString().replace("-", "")
                    + (ext.isEmpty() ? "" : "." + ext);
            Files.write(baseDir.resolve(key), data);
            return key;
        } catch (IOException e) {
            throw new BusinessException("文件存储失败:" + e.getMessage());
        }
    }

    @Override
    public byte[] load(String storagePath) {
        try {
            return Files.readAllBytes(baseDir.resolve(storagePath));
        } catch (IOException e) {
            throw new BusinessException("文件读取失败:" + e.getMessage());
        }
    }

    private String ext(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
