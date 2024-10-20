package com.aeterna.friendmall.search.controller;

import com.aeterna.friendmall.search.service.MallSearchService;
import com.aeterna.friendmall.search.vo.SearchParam;
import com.aeterna.friendmall.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.controller
 * @ClassName : .java
 * @createTime : 2024/7/24 1:23
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    // SpringMVC自动将页面提交过来的所有查询参数封装为指定的对象
    @GetMapping({ "/list.html"})
    public String listPage(SearchParam param, Model model, HttpServletRequest request) {

        String queryString = request.getQueryString();
        param.set_queryString(queryString);

        // 根据传递的页面查询参数去 ES 中检索
        SearchResult result = mallSearchService.search(param);

        model.addAttribute("result", result);

        return "list";
    }
}
