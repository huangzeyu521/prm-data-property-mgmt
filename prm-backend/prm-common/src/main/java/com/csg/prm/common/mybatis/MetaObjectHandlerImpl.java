package com.csg.prm.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.csg.prm.common.context.UserContext;
import com.csg.prm.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充:写入时自动补全创建人/时间、所属省/局(取自用户上下文)、逻辑删除位。
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        UserContext ctx = UserContextHolder.get();
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "creatorId", String.class, ctx.getUserId());
        this.strictInsertFill(metaObject, "updaterId", String.class, ctx.getUserId());
        this.strictInsertFill(metaObject, "provinceCode", String.class, ctx.getProvinceCode());
        this.strictInsertFill(metaObject, "bureauCode", String.class, ctx.getBureauCode());
        this.strictInsertFill(metaObject, "delFlag", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        UserContext ctx = UserContextHolder.get();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updaterId", String.class, ctx.getUserId());
    }
}
