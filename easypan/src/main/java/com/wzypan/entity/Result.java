package com.wzypan.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Result {
    private String status;//响应状态
    private String msg;  //响应信息 描述字符串
    private Object data; //返回的数据
    private Integer code; //响应码

    //增删改 成功响应
    public static Result success(){
        return new Result("success","请求成功",null, 200);
    }
    //查询 成功响应
    public static Result success(Object data){
        return new Result("success","请求成功",data, 200);
    }
    //失败响应
    public static Result error(String msg){
        return new Result("fail",msg,null, 404);
    }
}
