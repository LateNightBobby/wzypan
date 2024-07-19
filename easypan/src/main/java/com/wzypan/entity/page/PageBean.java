package com.wzypan.entity.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class PageBean {

    private Long totalCount;

    private Long pageSize;

    private Long pageNo;

    private Long pageTotal;

    private List list;

    public static PageBean convertFromPage(IPage page) {
        PageBean pageBean = new PageBean();
        pageBean.setTotalCount(page.getTotal()).setPageSize(page.getSize())
                .setPageNo(page.getCurrent()).setPageTotal(page.getPages());
        pageBean.setList(page.getRecords());
        return pageBean;
    }
}
