package com.argus.rag.document.readiness.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TopicProfileMapper {
    Map<String, Object> selectGroupProfile(@Param("groupId") Long groupId);

    List<String> selectReadyDocumentNames(@Param("groupId") Long groupId);
}
