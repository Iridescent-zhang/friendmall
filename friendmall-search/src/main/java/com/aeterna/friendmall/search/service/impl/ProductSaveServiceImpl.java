package com.aeterna.friendmall.search.service.impl;

import com.aeterna.common.to.es.SkuEsModel;
import com.aeterna.friendmall.search.config.FriendmallElasticSearchConfig;
import com.aeterna.friendmall.search.constant.EsConstant;
import com.aeterna.friendmall.search.service.ProductSaveService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.service.impl
 * @ClassName : .java
 * @createTime : 2024/7/17 16:23
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Service
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        // 保存到es
        // 1. 在es中建立索引product，建立映射关系

        // 2. 在es中保存数据
        // BulkRequest bulkRequest, RequestOptions options
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel skuEsModel : skuEsModels) {

            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);  // 要保存到哪个索引
            indexRequest.id(skuEsModel.getSkuId().toString());  // 数据id
            String jsonString = JSON.toJSONString(skuEsModel);
            indexRequest.source(jsonString, XContentType.JSON);  // 真正数据
            // 构造批量保存请求
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, FriendmallElasticSearchConfig.COMMON_OPTIONS);

        //TODO： 如果批量操作有失败的
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.info("商品上架成功：{}， 返回数据：{}", collect, bulk.toString());

        return  b;
    }
}
