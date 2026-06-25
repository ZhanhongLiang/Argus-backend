package com.argus.rag.document.readiness.mapper;

import com.argus.rag.document.readiness.model.entity.DocumentReadinessItemEntity;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessItemVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentReadinessItemMapper extends BaseMapper<DocumentReadinessItemEntity> {
    List<DocumentReadinessItemEntity> selectItemsByBatchId(@Param("batchId") Long batchId);

    List<DocumentReadinessItemEntity> selectImportableItems(@Param("batchId") Long batchId);
}
