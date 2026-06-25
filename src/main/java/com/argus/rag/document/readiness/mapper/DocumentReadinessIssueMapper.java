package com.argus.rag.document.readiness.mapper;

import com.argus.rag.document.readiness.model.entity.DocumentReadinessIssueEntity;
import com.argus.rag.document.readiness.model.vo.DocumentReadinessIssueVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentReadinessIssueMapper extends BaseMapper<DocumentReadinessIssueEntity> {
    List<DocumentReadinessIssueVO> selectByBatchId(@Param("batchId") Long batchId);
}
