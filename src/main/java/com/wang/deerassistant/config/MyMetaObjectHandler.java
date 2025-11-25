package com.wang.deerassistant.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        // createdAt = now (only when null)
        this.strictInsertFill(metaObject,
                "createdAt",
                LocalDateTime.class,
                LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 如果未来加 updatedAt，可在这里自动维护
        // this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
