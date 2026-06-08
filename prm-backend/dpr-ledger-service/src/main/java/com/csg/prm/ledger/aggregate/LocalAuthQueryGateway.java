package com.csg.prm.ledger.aggregate;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 授权域查询默认桩:返回空列表(离线/单测兜底,见 {@link LocalConfirmQueryGateway})。
 */
@Component
public class LocalAuthQueryGateway implements AuthQueryGateway {

    @Override
    public List<DomainRecord> findByAsset(String assetId) {
        return Collections.emptyList();
    }

    @Override
    public List<DomainRecord> pending() {
        return Collections.emptyList();
    }
}
