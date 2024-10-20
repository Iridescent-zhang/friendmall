package com.aeterna.common.constant;

import lombok.Data;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.common.constant
 * @ClassName : .java
 * @createTime : 2024/7/1 14:52
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
public class WareConstant {
    // 采购单状态码
    public enum PurchaseStatusEnum{
        CREATED(0,"新建"), ASSIGNED(1,"已分配"), RECEIVE(2,"已领取")
        , FINISH(3,"已分配"), HASERROR(4,"有异常");

        PurchaseStatusEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }

        private int code;
        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }

    //采购需求状态码
    public enum PurchaseDetailStatusEnum{
        CREATED(0,"新建"), ASSIGNED(1,"已分配"), BUYING(2,"正在采购")
        , FINISH(3,"已完成"), HASERROR(4,"采购失败");

        PurchaseDetailStatusEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }

        private int code;
        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
