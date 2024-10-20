package com.aeterna.friendmall.search;


import com.aeterna.friendmall.search.config.FriendmallElasticSearchConfig;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;    // 这是junit4的写法，这里用junit4了，把原来junit5删掉，重新导库就可以，4需要@RunWith，5应该不需要
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

@RunWith(SpringRunner.class)  // 在junit4下用来表示 用spring的驱动来跑单元测试
@SpringBootTest
public class  FriendmallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @ToString  // 让这些属性都具有tostring方法
    @Data
    static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;

    }


    @Test
    public void contextLoads() {
        System.out.println("client = " + client);
    }

    /**
     * 测试存储数据到ES
     * 更新也可以
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");  // 数据id
        // 写法一
//        indexRequest.source("userName","zhangsdan","age",18,"gender","男");  // source里是要保存的内容
        // 写法二 先new对象然后用fastjson转为json字符串 以后都用这种方式
        User user = new User();
        user.setUserName("zhangsdan");
        user.setAge(18);
        user.setGender("男");
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);

        // 使用客户端执行操作，返回响应（这是同步执行，异步是另一个函数）
        IndexResponse indexResponse = client.index(indexRequest, FriendmallElasticSearchConfig.COMMON_OPTIONS);

        // 提取有用的响应数据
        System.out.println("indexResponse = " + indexResponse);
    }

    @Data
    class User{
        private String userName;
        private Integer age;
        private String gender;
    }

    /**
     * 1. 创建检索请求
     * 2. 执行检索
     * 3. 分析结果
     */
    @Test
    public void searchData() throws IOException{
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("bank");  // 指定要查的索引

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 指定 DSL.json ，用searchSourceBuilder来构建检索条件
        /**
         * 1. 检索 query
         * QueryBuilders 是一个快速构造 QueryBuilder 的工具类
         */
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        /**
         * 2.1. 聚合 aggregation（聚合的意思是将query的结果聚合起来处理）
         * AggregationBuilders 是一个快速构造 AggregationBuilder 的工具类
         * 按照年龄的 值分布 进行聚合  第一个name是这个操作的名字，field才是对属性进行操作
         */
        TermsAggregationBuilder aggAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(aggAgg);

        /**
         * 2.2. 聚合 aggregation（聚合的意思是将query的结果聚合起来处理）
         * AggregationBuilders 是一个快速构造 AggregationBuilder 的工具类
         * 计算平均薪资  第一个name是这个操作的名字，field才是对属性进行操作
         */
        AvgAggregationBuilder balanceAvg = AggregationBuilders.avg("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        // 打印检索条件
        System.out.println("searchSourceBuilder = " + searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);

        // 执行检索
        SearchResponse searchResponse = client.search(searchRequest, FriendmallElasticSearchConfig.COMMON_OPTIONS);

        // 分析结果 searchResponse
        System.out.println("searchResponse = " + searchResponse.toString());
//        Map map = JSON.parseObject(searchResponse.toString(), Map.class);

        /**
         * 3.1 获取所有query查到的数据
         * 获取最大的 hits ： SearchHits hits = searchResponse.getHits();
         * 这个才是真正想要看的hits ： SearchHit[] searchHits = hits.getHits();
         */
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            /**
             *  "_index" : "bank",
             *         "_type" : "account",
             *         "_id" : "970",
             *         "_score" : 5.4032025,
             *         "_source" : { ... }
             */
            String sourceAsString = hit.getSourceAsString();
            Account account = JSON.parseObject(sourceAsString, Account.class);
            System.out.println("account = " + account);
        }

        /**
         * 3.2 获取所有聚合数据
         *
         */
        Aggregations aggregations = searchResponse.getAggregations();
//        for (Aggregation aggregation : aggregations.asList()) {
//            System.out.println("aggregation.getName() = " + aggregation.getName());
//        }

        /**
         * 直接通过聚合的名字去获取 聚合
         * "buckets" : [
         *         {
         *           "key" : 38,
         *           "doc_count" : 2
         *         },
         */
        Terms ageAgg = aggregations.get("ageAgg");
        for (Terms.Bucket bucket : ageAgg.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄分布key = " + keyAsString + "==>" + bucket.getDocCount());
        }

        Avg balanceAgg = aggregations.get("balanceAvg");
        System.out.println("平均薪资 = " + balanceAgg.getValue());

    }
}
