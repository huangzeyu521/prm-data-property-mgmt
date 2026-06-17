package com.csg.prm.confirm.aitool;

import com.csg.prm.confirm.aitool.entity.AitMaterial;
import com.csg.prm.confirm.aitool.service.AitMaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 消费校验:把生成的测试材料逐个喂回真实 uploadBinary+parse 流水线,断言可解析。
 * 仅当传 -Dverify.base=<宿主test目录> 时运行。
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "verify.base", matches = ".+")
class TestMaterialConsumeTest {

    private static final Set<String> EXT = Set.of("pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png");

    @Autowired private AitMaterialService materialService;

    @Test
    void consumeAllMaterials() throws Exception {
        File root = new File(System.getProperty("verify.base"), "智能确权辅助工具");
        assertTrue(root.isDirectory(), "材料目录不存在:" + root);

        List<Path> files;
        try (Stream<Path> w = Files.walk(root.toPath())) {
            files = w.filter(Files::isRegularFile)
                    .filter(p -> EXT.contains(ext(p.getFileName().toString())))
                    .sorted()
                    .toList();
        }
        assertFalse(files.isEmpty(), "未找到任何材料文件");

        int ok = 0;
        int failed = 0;
        List<String> unexpected = new ArrayList<>();
        String dupChildId = null;
        for (Path p : files) {
            String name = p.getFileName().toString();
            byte[] bytes = Files.readAllBytes(p);
            String id;
            try {
                id = materialService.uploadBinary(name, bytes, null, null, null);
            } catch (RuntimeException e) {
                unexpected.add(name + " 上传失败:" + e.getMessage());
                continue;
            }
            // 同步解析(sync),失败不抛(parse 内部对 async=false 会抛 → 包裹)
            String status;
            try {
                materialService.parse(id);
            } catch (RuntimeException ignore) {
                // parse 同步路径对损坏材料会抛 BizException,状态已落"失败"
            }
            AitMaterial m = materialService.getMaterial(id);
            status = m.getParseStatus();
            boolean expectFail = name.contains("空白");
            if (expectFail) {
                if (!AitMaterial.PARSE_FAILED.equals(status)) {
                    unexpected.add(name + " 预期失败,实际=" + status);
                }
            } else {
                if (!AitMaterial.PARSE_SUCCESS.equals(status)) {
                    unexpected.add(name + " 预期成功,实际=" + status + " 原因=" + m.getFailReason());
                }
            }
            if (AitMaterial.PARSE_SUCCESS.equals(status)) {
                ok++;
            } else {
                failed++;
            }
            if (name.contains("副本")) {
                dupChildId = m.getDuplicateOf();
            }
            System.out.println("[VERIFY] " + name + " → " + status
                    + (m.getOcrUsed() != null && m.getOcrUsed() == 1 ? " (OCR)" : "")
                    + (m.getDuplicateOf() != null ? " (重复→" + m.getDuplicateOf() + ")" : ""));
        }
        System.out.println("[VERIFY] 合计 " + files.size() + ":成功 " + ok + " / 失败(含负面) " + failed);

        assertTrue(unexpected.isEmpty(), "存在非预期结果:\n" + String.join("\n", unexpected));
        assertTrue(dupChildId != null, "重复检测:副本应命中原件(duplicateOf 非空)");
    }

    private static String ext(String n) {
        int i = n.lastIndexOf('.');
        return i < 0 ? "" : n.substring(i + 1).toLowerCase();
    }
}
