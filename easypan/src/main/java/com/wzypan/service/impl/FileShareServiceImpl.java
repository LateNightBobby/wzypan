package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileShare;
import com.wzypan.mapper.FileShareMapper;
import com.wzypan.service.FileShareService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wzy
 * @since 2024-08-28
 */
@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements FileShareService {

    @Resource
    private FileShareMapper fileShareMapper;

    @Override
    public PageBean pageShareList(String userId, PageQuery pageQuery) {
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileShare::getUserId, userId);
        Page<FileShare> page = new Page<>(pageQuery.getPageNo()==null? 1: pageQuery.getPageNo(), pageQuery.getPageSize()==null? 15: pageQuery.getPageSize());
        IPage iPage = fileShareMapper.selectPage(page, wrapper);
        return PageBean.convertFromPage(iPage);
    }
}
