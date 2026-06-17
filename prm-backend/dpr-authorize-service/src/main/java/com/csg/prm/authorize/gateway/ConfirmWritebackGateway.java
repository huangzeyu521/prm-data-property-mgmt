package com.csg.prm.authorize.gateway;

/**
 * 资产卡片写回网关(授权侧→确权服务)端口。
 * 授权生效后,触发确权服务把该资产的产权/权益结论写回数据资产管理平台卡片(集成统一收口在确权服务)。
 * 生产由 {@link HttpConfirmWritebackGateway} 跨服务调用;本地/测试用 {@link LocalConfirmWritebackGateway} 桩。
 */
public interface ConfirmWritebackGateway {

    /** 触发某资产的卡片写回。实现须非致命:失败不得影响授权主流程。 */
    void writeback(String assetId);
}
