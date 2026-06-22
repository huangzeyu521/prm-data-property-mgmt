package com.csg.prm.authorize.ai;

import com.csg.prm.common.api.R;
import com.csg.prm.common.aitrace.AiRunLog;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 授权大模型校验机制接口(南网"大模型校验机制完善"授权侧):
 * 校验规则可视化 / 校验过程回放 / 防篡改快照固化与验真。
 */
@RestController
@RequestMapping("/api/dpr/auth/ai")
public class AuthAiController {

    private final AuthAiService service;

    public AuthAiController(AuthAiService service) {
        this.service = service;
    }

    /** §1 校验规则可视化:逐应交项的校验逻辑 + 规则明细 + AI 判定依据。 */
    @GetMapping("/check-logic")
    public R<AuthAiService.CheckLogic> checkLogic(@RequestParam String applyId) {
        return R.ok(service.checkLogic(applyId));
    }

    /** §2 校验过程回放:该授权申请全部大模型操作留痕时间线(能力/模型/耗时/SM3/触发人)。 */
    @GetMapping("/runlog")
    public R<List<AiRunLog>> runlog(@RequestParam String applyId) {
        return R.ok(service.runlog(applyId));
    }

    /** §2 固化防篡改 AI 校验快照(服务端 SM3 + 上链 + 关联留痕),供人工审核复核·可审计。 */
    @PostMapping("/{applyId}/ai-snapshot")
    public R<Void> saveSnapshot(@PathVariable String applyId, @RequestBody String snapshotJson) {
        service.saveSnapshot(applyId, snapshotJson);
        return R.ok();
    }

    /** §2 校验已固化快照完整性(重算 SM3 比对存证),防篡改验真。 */
    @GetMapping("/{applyId}/ai-snapshot/verify")
    public R<Map<String, Object>> verifySnapshot(@PathVariable String applyId) {
        return R.ok(service.verifySnapshot(applyId));
    }
}
