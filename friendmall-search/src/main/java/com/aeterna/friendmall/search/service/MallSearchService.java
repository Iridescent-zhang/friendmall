package com.aeterna.friendmall.search.service;

import com.aeterna.friendmall.search.vo.SearchParam;
import com.aeterna.friendmall.search.vo.SearchResult;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.service
 * @ClassName : .java
 * @createTime : 2024/7/24 11:41
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
public interface MallSearchService {
    /**
     * @param param 检索的所有参数
     * @return 返回自己封装的检索结果，里面包含页面所有信息
     */
    SearchResult search(SearchParam param);
}
