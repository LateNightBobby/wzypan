package com.wzypan.entity.page;

import lombok.Data;

@Data
public class PageQuery {
    //页码
    private Integer pageNo = 1;
    //页大小
    private Integer pageSize = 15;
}
