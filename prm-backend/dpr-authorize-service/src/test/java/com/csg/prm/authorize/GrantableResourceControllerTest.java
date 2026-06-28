package com.csg.prm.authorize;

import com.csg.prm.authorize.controller.GrantableResourceController;
import com.csg.prm.authorize.gateway.LocalOpenCatalogGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可授权资源池-对外开放目录过滤单测(纯单元,不起 Spring 上下文)。
 * 验证经营权资源池前置裁剪:在对外开放目录的资产保留,NONOPEN 前缀(桩约定"非对外开放")剔除。
 */
class GrantableResourceControllerTest {

    private final GrantableResourceController controller =
            new GrantableResourceController(new LocalOpenCatalogGateway());

    @Test
    @DisplayName("openFilter 保留对外开放资产、剔除非对外开放(NONOPEN)")
    void openFilter_keepsOpen_dropsNonOpen() {
        List<String> res = controller.openFilter(List.of("AST-002", "NONOPEN-X", "AST-006")).getData();
        assertEquals(List.of("AST-002", "AST-006"), res);
    }

    @Test
    @DisplayName("openFilter 去重并保持入参顺序")
    void openFilter_dedupKeepsOrder() {
        List<String> res = controller.openFilter(Arrays.asList("AST-006", "AST-002", "AST-006")).getData();
        assertEquals(List.of("AST-006", "AST-002"), res);
    }

    @Test
    @DisplayName("openFilter 入参为空返回空集合,不抛异常")
    void openFilter_null_returnsEmpty() {
        assertTrue(controller.openFilter(null).getData().isEmpty());
    }
}
