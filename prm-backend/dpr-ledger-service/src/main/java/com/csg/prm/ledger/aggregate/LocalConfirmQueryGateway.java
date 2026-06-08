package com.csg.prm.ledger.aggregate;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 确权域查询默认桩:返回空列表。保证台账服务在未开启跨服务聚合(离线/单测)时
 * 资产360与待办中心仍可正常返回(仅含台账本地数据),不引入外部依赖。
 */
@Component
public class LocalConfirmQueryGateway implements ConfirmQueryGateway {

    @Override
    public List<DomainRecord> findByAsset(String assetId) {
        return Collections.emptyList();
    }

    @Override
    public List<DomainRecord> pending() {
        return Collections.emptyList();
    }
}
