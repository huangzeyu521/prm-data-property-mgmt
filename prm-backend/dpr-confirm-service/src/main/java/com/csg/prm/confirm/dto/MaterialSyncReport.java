package com.csg.prm.confirm.dto;

import java.util.List;

/**
 * 确权材料"先从平台元数据同步已上传材料"报告。
 *
 * <p>从数据资产管理平台 AU_TABLE_META_DATA(产权元数据表信息)按 A–J 维度带入平台已上传材料附件,
 * 命中的应交项自动登记为"平台同步"免上传;返回同步明细与仍待用户补全的清单。</p>
 */
public class MaterialSyncReport {

    private String applyId;
    private int syncedCount;            // 本次新同步登记的材料数(不含此前已存在的)
    private List<SyncedItem> synced;    // 已从平台同步的材料明细(应交项名 + 平台附件名)
    private List<String> stillMissing;  // 平台未覆盖、仍需用户补全的应交项(必填优先)
    private String summary;

    /** 单条平台同步材料:应交项名称 + 平台附件名 + 触发标识(A–J/证明)。 */
    public static class SyncedItem {
        private String materialName;
        private String attachment;
        private String code;

        public SyncedItem() {
        }

        public SyncedItem(String materialName, String attachment, String code) {
            this.materialName = materialName;
            this.attachment = attachment;
            this.code = code;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public String getAttachment() {
            return attachment;
        }

        public void setAttachment(String attachment) {
            this.attachment = attachment;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public int getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(int syncedCount) {
        this.syncedCount = syncedCount;
    }

    public List<SyncedItem> getSynced() {
        return synced;
    }

    public void setSynced(List<SyncedItem> synced) {
        this.synced = synced;
    }

    public List<String> getStillMissing() {
        return stillMissing;
    }

    public void setStillMissing(List<String> stillMissing) {
        this.stillMissing = stillMissing;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
