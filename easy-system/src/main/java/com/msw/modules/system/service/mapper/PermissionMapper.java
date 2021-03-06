package com.msw.modules.system.service.mapper;

import com.msw.mapper.EntityMapper;
import com.msw.modules.system.domain.Permission;
import com.msw.modules.system.service.dto.PermissionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * @author mashuangwei
 * @date 2018-11-23
 */
@Mapper(componentModel = "spring",unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PermissionMapper extends EntityMapper<PermissionDTO, Permission> {

}
