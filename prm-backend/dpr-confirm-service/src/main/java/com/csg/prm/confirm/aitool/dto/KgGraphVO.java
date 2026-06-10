package com.csg.prm.confirm.aitool.dto;

import com.csg.prm.confirm.aitool.entity.AitConflict;

import java.util.List;

/**
 * 电力权属知识图谱结构化视图(可研 M2-#9):
 * 节点 type ∈ {主体, 客体, 授权事项, 有效期};关系 relation ∈ {授权, 归属, 有效期, 冲突}。
 * 由权属主张(AitKgClaim)+冲突(AitConflict)物化,关系数据结构,非独立图数据库。
 */
public class KgGraphVO {

    private String assetId;
    private List<Node> nodes;
    private List<Edge> edges;
    private List<AitConflict> conflicts;

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
    public List<Node> getNodes() { return nodes; }
    public void setNodes(List<Node> nodes) { this.nodes = nodes; }
    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }
    public List<AitConflict> getConflicts() { return conflicts; }
    public void setConflicts(List<AitConflict> conflicts) { this.conflicts = conflicts; }

    /** 图谱节点:type=主体/客体/授权事项/有效期。 */
    public record Node(String id, String type, String label) {
    }

    /** 图谱边:relation=授权/归属/有效期/冲突。 */
    public record Edge(String from, String to, String relation, String label) {
    }
}
