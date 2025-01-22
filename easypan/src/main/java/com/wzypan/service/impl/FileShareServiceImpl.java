package com.wzypan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.FileShareInfoDto;
import com.wzypan.entity.dto.SessionShareDto;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.entity.enums.ShareValidTypeEnums;
import com.wzypan.entity.page.PageBean;
import com.wzypan.entity.page.PageQuery;
import com.wzypan.entity.po.FileShare;
import com.wzypan.exception.BusinessException;
import com.wzypan.mapper.FileShareMapper;
import com.wzypan.service.FileShareService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wzypan.utils.DateUtil;
import com.wzypan.utils.StringTools;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
//        IPage iPage = fileShareMapper.selectPage(page, wrapper);
        List<FileShareInfoDto> fileShares = fileShareMapper.selectPageWithJoin(page, userId);
        return PageBean.convertFromPage(page).setList(fileShares);
    }

    @Override
    public void saveShare(FileShare fileShare) {
        ShareValidTypeEnums typeEnums = ShareValidTypeEnums.getByType(fileShare.getValidType());
        if (null==typeEnums) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (typeEnums != ShareValidTypeEnums.FOREVER) {
            fileShare.setExpireTime(DateUtil.getAfterDate(typeEnums.getDays()));
        }
        Date curDate = new Date();
        fileShare.setShareTime(curDate);
        if (StringTools.isEmpty(fileShare.getCode())) {
            fileShare.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        fileShare.setShareId(StringTools.getRandomString(20));
        fileShareMapper.insert(fileShare);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFileShareBatch(String[] shareIdArray, String userId) {
        if (shareIdArray == null || shareIdArray.length == 0) {
            throw new IllegalArgumentException("shareIdArray cannot be null or empty");
        }
        LambdaQueryWrapper<FileShare> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FileShare::getShareId, Arrays.asList(shareIdArray)).eq(FileShare::getUserId, userId);
        int count = fileShareMapper.delete(wrapper);
        if (count != shareIdArray.length) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShare fileShare = fileShareMapper.selectById(shareId);
        if (fileShare==null || fileShare.getExpireTime() != null && new Date().after(fileShare.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg());
        }

        if (!fileShare.getCode().equals(code)) {
            throw new BusinessException("wrong share code");
        }

        //更新访问次数,可能存在并发
        fileShareMapper.incrShareShowCount(shareId);

        SessionShareDto sessionShareDto = new SessionShareDto().setShareUserId(fileShare.getUserId())
                .setFileId(fileShare.getFileId()).setExpireTime(fileShare.getExpireTime()).setShareId(shareId);
        return sessionShareDto;
    }
}
