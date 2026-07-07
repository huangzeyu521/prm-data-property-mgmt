package com.csg.prm.confirm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.csg.prm.common.api.PageResult;
import com.csg.prm.common.api.ResponseCode;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import com.csg.prm.common.exception.BusinessException;
import com.csg.prm.confirm.dto.RecheckHealthVO;
import com.csg.prm.confirm.dto.RecheckTaskQuery;
import com.csg.prm.confirm.entity.ConfirmRecheckTask;
import com.csg.prm.confirm.entity.EquityCard;
import com.csg.prm.confirm.mapper.ConfirmRecheckTaskMapper;
import com.csg.prm.confirm.mapper.EquityCardMapper;
import com.csg.prm.confirm.service.ConfirmApplyService;
import com.csg.prm.confirm.service.ConfirmRecheckTaskService;
import com.csg.prm.confirm.service.EquityCardService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConfirmRecheckTaskServiceImpl implements ConfirmRecheckTaskService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfirmRecheckTaskServiceImpl.class);

    /** 未销号状态(幂等去重口径):同资产×同类型仅一张活动工单 */
    private static final List<String> OPEN_STATUSES =
            List.of(ConfirmRecheckTask.STATUS_OPEN, ConfirmRecheckTask.STATUS_CHANGING);
    /** 非到期扫描来源的默认处置期限(天) */
    private static final int DEFAULT_DUE_DAYS = 30;

    private final ConfirmRecheckTaskMapper mapper;
    private final EquityCardMapper cardMapper;
    private final EquityCardService cardService;
    // 懒解析防循环:deriveChange → ConfirmApplyService.createReConfirm,而 ApplyServiceImpl 又依赖本服务销号
    private final ObjectProvider<ConfirmApplyService> applyServiceProvider;

    public ConfirmRecheckTaskServiceImpl(ConfirmRecheckTaskMapper mapper, EquityCardMapper cardMapper,
                                         EquityCardService cardService,
                                         ObjectProvider<ConfirmApplyService> applyServiceProvider) {
        this.mapper = mapper;
        this.cardMapper = cardMapper;
        this.cardService = cardService;
        this.applyServiceProvider = applyServiceProvider;
    }

    @Override
    @Transactional
    public String createTask(ConfirmRecheckTask task) {
        if (!StringUtils.hasText(task.getAssetId())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(), "工单关联资产ID不能为空");
        }
        ConfirmRecheckTask exist = findOpen(task.getAssetId(), task.getTaskType(), task.getRefNo());
        if (exist != null) {
            return exist.getTaskId();
        }
        if (!StringUtils.hasText(task.getTaskType())) {
            task.setTaskType(ConfirmRecheckTask.TYPE_RECHECK);
        }
        if (!StringUtils.hasText(task.getStatus())) {
            task.setStatus(ConfirmRecheckTask.STATUS_OPEN);
        }
        if (task.getDueDate() == null) {
            task.setDueDate(LocalDateTime.now().plusDays(DEFAULT_DUE_DAYS));
        }
        task.setTaskNo(generateTaskNo(task.getTaskType()));
        mapper.insert(task);
        return task.getTaskId();
    }

    /** 活动工单幂等定位:重确权按 资产×类型;授权处置按 资产×类型×受影响授权(refNo)。 */
    private ConfirmRecheckTask findOpen(String assetId, String taskType, String refNo) {
        String type = StringUtils.hasText(taskType) ? taskType : ConfirmRecheckTask.TYPE_RECHECK;
        LambdaQueryWrapper<ConfirmRecheckTask> w = new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getAssetId, assetId)
                .eq(ConfirmRecheckTask::getTaskType, type)
                .in(ConfirmRecheckTask::getStatus, OPEN_STATUSES);
        if (ConfirmRecheckTask.TYPE_AUTH_DISPOSAL.equals(type) && StringUtils.hasText(refNo)) {
            w.eq(ConfirmRecheckTask::getRefNo, refNo);
        }
        return mapper.selectOne(w.last("LIMIT 1"));
    }

    @Override
    public PageResult<ConfirmRecheckTask> page(RecheckTaskQuery query) {
        LambdaQueryWrapper<ConfirmRecheckTask> w = new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(StringUtils.hasText(query.getStatus()), ConfirmRecheckTask::getStatus, query.getStatus())
                .eq(StringUtils.hasText(query.getTaskType()), ConfirmRecheckTask::getTaskType, query.getTaskType())
                .eq(StringUtils.hasText(query.getSource()), ConfirmRecheckTask::getSource, query.getSource())
                .like(StringUtils.hasText(query.getAssetName()), ConfirmRecheckTask::getAssetName, query.getAssetName())
                .orderByAsc(ConfirmRecheckTask::getStatus)
                .orderByAsc(ConfirmRecheckTask::getDueDate);
        IPage<ConfirmRecheckTask> page = mapper.selectPage(query.toPage(), w);
        return PageResult.of(page);
    }

    @Override
    public ConfirmRecheckTask getById(String taskId) {
        ConfirmRecheckTask task = mapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResponseCode.NOT_FOUND.getCode(), "工单不存在:" + taskId);
        }
        return task;
    }

    @Override
    @Transactional
    public int scanDueCards(int days) {
        List<EquityCard> due = cardService.listReConfirmDue(days);
        int created = 0;
        for (EquityCard card : due) {
            ConfirmRecheckTask task = new ConfirmRecheckTask();
            task.setTaskType(ConfirmRecheckTask.TYPE_RECHECK);
            task.setAssetId(card.getAssetId());
            task.setAssetName(card.getAssetName());
            task.setSource(ConfirmRecheckTask.SOURCE_QUARTER_SCAN);
            task.setTriggerType("权益到期");
            task.setReason("权益卡片 " + card.getCardNo() + "(" + card.getRightType() + ")有效期至 "
                    + (card.getValidDate() == null ? "未设" : card.getValidDate().toLocalDate())
                    + ",按35号文§二(三)2须重新确权");
            task.setDueDate(card.getValidDate() != null ? card.getValidDate() : LocalDateTime.now().plusDays(DEFAULT_DUE_DAYS));
            task.setRefNo(card.getCardNo());
            // 同资产幂等:多张到期卡片收敛一张重确权工单(变更单本就按资产整单修订)
            String before = findOpenTaskId(card.getAssetId());
            String id = createTask(task);
            if (before == null && id != null) {
                created++;
            }
        }
        log.info("[季度重确权扫描] 到期/临期权益卡片 {} 张,新建重确权工单 {} 张(其余幂等归并既有工单)", due.size(), created);
        return created;
    }

    private String findOpenTaskId(String assetId) {
        ConfirmRecheckTask t = findOpen(assetId, ConfirmRecheckTask.TYPE_RECHECK, null);
        return t == null ? null : t.getTaskId();
    }

    @Override
    @Transactional
    public String deriveChange(String taskId) {
        ConfirmRecheckTask task = getById(taskId);
        if (!ConfirmRecheckTask.TYPE_RECHECK.equals(task.getTaskType())) {
            throw new BusinessException("仅「重确权」工单可派生确权变更草稿;授权处置工单请前往授权管理处置后销号");
        }
        if (!ConfirmRecheckTask.STATUS_OPEN.equals(task.getStatus())) {
            throw new BusinessException("仅「待处置」工单可派生变更草稿,当前状态:" + task.getStatus());
        }
        ConfirmApplyService applyService = applyServiceProvider.getObject();
        String applyId = applyService.createReConfirm(task.getAssetId(), task.getAssetName(), null,
                "重确权工单派生:" + task.getReason(), "RECHECK:" + task.getTaskNo(), task.getTriggerType());
        task.setStatus(ConfirmRecheckTask.STATUS_CHANGING);
        task.setApplyId(applyId);
        fillHandler(task, null);
        mapper.updateById(task);
        return applyId;
    }

    @Override
    @Transactional
    public void confirmNoChange(String taskId, String note) {
        if (!StringUtils.hasText(note)) {
            throw new BusinessException(ResponseCode.PARAM_ERROR.getCode(),
                    "「复核确认无变化」须填写复核结论(重新确权的合法产出之一,必须留痕方可销号)");
        }
        ConfirmRecheckTask task = getById(taskId);
        if (!ConfirmRecheckTask.STATUS_OPEN.equals(task.getStatus())) {
            throw new BusinessException("仅「待处置」工单可复核销号,当前状态:" + task.getStatus());
        }
        task.setStatus(ConfirmRecheckTask.STATUS_NO_CHANGE);
        fillHandler(task, note);
        mapper.updateById(task);
    }

    @Override
    @Transactional
    public void complete(String taskId, String note) {
        ConfirmRecheckTask task = getById(taskId);
        if (!OPEN_STATUSES.contains(task.getStatus())) {
            throw new BusinessException("工单已销号,状态:" + task.getStatus());
        }
        task.setStatus(ConfirmRecheckTask.STATUS_DONE);
        fillHandler(task, StringUtils.hasText(note) ? note : "处置完成销号");
        mapper.updateById(task);
    }

    @Override
    @Transactional
    public void completeByApply(String applyId) {
        if (!StringUtils.hasText(applyId)) {
            return;
        }
        List<ConfirmRecheckTask> linked = mapper.selectList(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getApplyId, applyId)
                .eq(ConfirmRecheckTask::getStatus, ConfirmRecheckTask.STATUS_CHANGING));
        for (ConfirmRecheckTask task : linked) {
            task.setStatus(ConfirmRecheckTask.STATUS_DONE);
            fillHandler(task, "派生确权变更单终审生效,自动销号");
            mapper.updateById(task);
        }
    }

    @Override
    @Transactional
    public void linkDerivedApply(String assetId, String assetName, String applyId,
                                 String triggerType, String reason, String refNo) {
        if (!StringUtils.hasText(assetId) || !StringUtils.hasText(applyId)) {
            return;
        }
        ConfirmRecheckTask open = findOpen(assetId, ConfirmRecheckTask.TYPE_RECHECK, null);
        if (open != null && ConfirmRecheckTask.STATUS_OPEN.equals(open.getStatus())) {
            open.setStatus(ConfirmRecheckTask.STATUS_CHANGING);
            open.setApplyId(applyId);
            fillHandler(open, "外部联动派生变更草稿,回链在池工单");
            mapper.updateById(open);
            return;
        }
        if (open != null) {
            return; // 已有变更申请中的工单,不重复登记
        }
        ConfirmRecheckTask task = new ConfirmRecheckTask();
        task.setTaskType(ConfirmRecheckTask.TYPE_RECHECK);
        task.setAssetId(assetId);
        task.setAssetName(assetName);
        task.setSource(ConfirmRecheckTask.SOURCE_MONITOR);
        task.setTriggerType(triggerType);
        task.setReason(StringUtils.hasText(reason) ? reason : "监测联动派生重确权");
        task.setStatus(ConfirmRecheckTask.STATUS_CHANGING);
        task.setApplyId(applyId);
        task.setRefNo(refNo);
        task.setDueDate(LocalDateTime.now().plusDays(DEFAULT_DUE_DAYS));
        task.setTaskNo(generateTaskNo(task.getTaskType()));
        mapper.insert(task);
    }

    @Override
    public RecheckHealthVO health() {
        RecheckHealthVO vo = new RecheckHealthVO();
        vo.setOpen(count(ConfirmRecheckTask.STATUS_OPEN));
        vo.setChanging(count(ConfirmRecheckTask.STATUS_CHANGING));
        vo.setNoChange(count(ConfirmRecheckTask.STATUS_NO_CHANGE));
        vo.setDone(count(ConfirmRecheckTask.STATUS_DONE));
        vo.setOverdue(mapper.selectCount(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getStatus, ConfirmRecheckTask.STATUS_OPEN)
                .lt(ConfirmRecheckTask::getDueDate, LocalDateTime.now())));
        vo.setDueSoonCards(cardService.listReConfirmDue(90).size());
        // 版本链完整率:v2+ 卡片必须带 supersededCardNo 回链(被取代链断裂=不可追溯)
        List<EquityCard> versioned = cardMapper.selectList(new LambdaQueryWrapper<EquityCard>()
                .gt(EquityCard::getVersion, 1));
        long chained = versioned.stream().filter(c -> StringUtils.hasText(c.getSupersededCardNo())).count();
        vo.setChainIntegrityRate(versioned.isEmpty() ? 100 : (int) Math.round(chained * 100.0 / versioned.size()));
        // 按期处置率:已销号(无变化/完成)工单中 handleTime<=dueDate 占比
        List<ConfirmRecheckTask> closed = mapper.selectList(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .in(ConfirmRecheckTask::getStatus,
                        List.of(ConfirmRecheckTask.STATUS_NO_CHANGE, ConfirmRecheckTask.STATUS_DONE)));
        long onTime = closed.stream()
                .filter(t -> t.getHandleTime() != null && t.getDueDate() != null
                        && !t.getHandleTime().isAfter(t.getDueDate()))
                .count();
        vo.setOnTimeRate(closed.isEmpty() ? 100 : (int) Math.round(onTime * 100.0 / closed.size()));
        return vo;
    }

    private long count(String status) {
        return mapper.selectCount(new LambdaQueryWrapper<ConfirmRecheckTask>()
                .eq(ConfirmRecheckTask::getStatus, status));
    }

    private void fillHandler(ConfirmRecheckTask task, String note) {
        UserContext ctx = UserContextHolder.get();
        task.setHandlerId(ctx == null ? "system" : ctx.getUserId());
        task.setHandlerName(ctx == null || !StringUtils.hasText(ctx.getUserName()) ? "系统" : ctx.getUserName());
        task.setHandleTime(LocalDateTime.now());
        if (note != null) {
            task.setHandleNote(note);
        }
    }

    private String generateTaskNo(String taskType) {
        String prefix = ConfirmRecheckTask.TYPE_AUTH_DISPOSAL.equals(taskType) ? "AD" : "RC";
        String date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        int seq = (int) (Math.abs(java.util.UUID.randomUUID().getLeastSignificantBits()) % 100000);
        return String.format("%s-%s-%05d", prefix, date, seq);
    }
}
