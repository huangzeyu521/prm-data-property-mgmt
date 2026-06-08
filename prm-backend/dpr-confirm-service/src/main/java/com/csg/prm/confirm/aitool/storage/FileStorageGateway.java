package com.csg.prm.confirm.aitool.storage;

/**
 * 确权材料文件存储网关(端口)。默认本地文件系统实现({@link LocalFileStorageGateway});
 * 生产可由对象存储(华为 OBS / MinIO)实现 @Primary 覆盖,业务代码不感知。
 */
public interface FileStorageGateway {

    /** 保存文件,返回存储相对路径/键 */
    String save(String fileName, byte[] data);

    /** 读取文件字节 */
    byte[] load(String storagePath);
}
