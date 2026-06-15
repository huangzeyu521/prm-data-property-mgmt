package com.csg.prm.confirm.aitool.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** multipart 文件名编码还原(修复 ISO-8859-1 误解码导致的中文乱码)。 */
class AitMaterialControllerTest {

    @Test
    @DisplayName("被 ISO-8859-1 误解码的中文文件名应还原为 UTF-8")
    void fixFilename_recoversMojibake() {
        String original = "确权证明.docx";
        // 模拟 Tomcat:UTF-8 字节被当作 ISO-8859-1 读入字符串
        String mojibake = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        assertEquals(original, AitMaterialController.fixFilename(mojibake));
    }

    @Test
    @DisplayName("已含真实 CJK 字符的文件名应原样保留")
    void fixFilename_keepsRealCjk() {
        assertEquals("权属证明.png", AitMaterialController.fixFilename("权属证明.png"));
    }

    @Test
    @DisplayName("纯 ASCII 文件名应原样保留")
    void fixFilename_keepsAscii() {
        assertEquals("OCR-SAMPLE-20260615.pdf", AitMaterialController.fixFilename("OCR-SAMPLE-20260615.pdf"));
    }

    @Test
    @DisplayName("null/空名安全返回")
    void fixFilename_nullSafe() {
        assertNull(AitMaterialController.fixFilename(null));
        assertEquals("", AitMaterialController.fixFilename(""));
    }
}
