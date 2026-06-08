package com.csg.prm.authorize.dto;

import com.csg.prm.common.query.PageQuery;

public class AuthApplyQuery extends PageQuery {

    private String assetName;
    private String authMode;
    private String status;
    private String granteeOrg;

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGranteeOrg() {
        return granteeOrg;
    }

    public void setGranteeOrg(String granteeOrg) {
        this.granteeOrg = granteeOrg;
    }
}
