package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.FileCategoryEnum;
import com.wzypan.entity.enums.FileDelFlagEnum;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileInfo;
import com.wzypan.mapper.FileInfoMapper;
import com.wzypan.service.FileInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * 文件信息表 服务实现类
 * </p>
 *
 * @author wzy
 * @since 2024-07-17
 */
@Service
@Slf4j
public class FileInfoServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements FileInfoService {

    @Resource
    private FileInfoMapper fileInfoMapper;

    @Override
    public PageBean pageDataList(PageQuery pageQuery, SessionWebUserDto userDto, FileCategoryEnum categoryEnum, String filePid, String fileNameFuzzy) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<>();
        //code为null 即为all 选择所有文件
        if (categoryEnum!=null) {
            wrapper.eq(FileInfo::getFileCategory, categoryEnum.getCode());
        }
        wrapper .eq(FileInfo::getUserId, userDto.getUserId())
                .eq(FileInfo::getDelFlag, FileDelFlagEnum.USING.getFlag())
                .eq(FileInfo::getFilePid, filePid).like(!fileNameFuzzy.isEmpty() && !fileNameFuzzy.isBlank(), FileInfo::getFileName, fileNameFuzzy);
        wrapper.orderByDesc(FileInfo::getLastUpdateTime);
        Page<FileInfo> page = new Page<>(pageQuery.getPageNo()==null? 1: pageQuery.getPageNo(), pageQuery.getPageSize()==null? 15: pageQuery.getPageSize());
        IPage<FileInfo> filePage = fileInfoMapper.selectPage(page, wrapper);
        return PageBean.convertFromPage(filePage);
    }
}
