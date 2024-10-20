package com.aeterna.friendmall.search.service.impl;

import com.aeterna.common.to.es.SkuEsModel;
import com.aeterna.common.utils.R;
import com.aeterna.friendmall.search.config.FriendmallElasticSearchConfig;
import com.aeterna.friendmall.search.constant.EsConstant;
import com.aeterna.friendmall.search.feign.ProductFeignService;
import com.aeterna.friendmall.search.service.MallSearchService;
import com.aeterna.friendmall.search.vo.AttrResponseVo;
import com.aeterna.friendmall.search.vo.BrandVo;
import com.aeterna.friendmall.search.vo.SearchParam;
import com.aeterna.friendmall.search.vo.SearchResult;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
 * @createTime : 2024/7/24 11:41
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    // ES REST API 的客户端
    @Autowired
    RestHighLevelClient restHighLevelClient;


    @Autowired
    ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam param) {
        SearchResult result = null;
        // 1）创建检索请求，我们抽取成方法：private SearchRequest buildSearchRequest()
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            // 2）执行检索得到响应
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest, FriendmallElasticSearchConfig.COMMON_OPTIONS);
            // 3）分析响应数据并封装为我们的输出格式：SearchResult，同样抽取为方法
            result = buildSearchResult(searchResponse, param);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * 准备检索请求
     * # keyword模糊匹配，过滤（属性、分类、品牌、价格区间、库存），排序，分页，高亮
     *      ，聚合分析(商品可选属性是动态变化，要聚合分析)
     */
    private SearchRequest buildSearchRequest(SearchParam param) {

        // 根据 DSL.json ，用searchSourceBuilder来构建检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        /**
         * 1）检索 query ： keyword模糊匹配，过滤（属性、分类、品牌、价格区间、库存）（其实过滤就是匹配，只不过不参与热度评分）
         * QueryBuilders 是一个快速构造 QueryBuilder 的工具类
         */
        // 1. 构建总bool-query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // 1.1 bool-must : keyword模糊匹配
        if (!StringUtils.isEmpty(param.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2 bool-filter : 按照三级分类id getCatalog3Id 查询
        if (param.getCatalog3Id()!=null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2 bool-filter : 按照品牌id brandId 查询
        if (param.getBrandId()!=null && param.getBrandId().size()>0) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2 bool-filter : 按照属性 查询
        if (param.getAttrs()!=null && param.getAttrs().size()>0) {
            // attrs=1_3G:4G:5G & attrs=2_骁龙845 & attrs=4_高清屏
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                // attrStr = 1_3G:4G
                String[] s = attrStr.split("_");
                String attrId = s[0];  // 属性id
                String[] attrValues = s[1].split(":");  // 属性的可取值
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                // 每一句 attrs=1_3G:4G:5G 都要生成nestedQueryBuilder，并放到boolQueryBuilder里
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }

        // 1.2 bool-filter : 按照是否有库存查询
        if (param.getHasStock() != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock()==1));
        }

        // 1.2 bool-filter : 按照价格区间查询
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            // skuPrice = 500_1000/_1000/500_
            String[] split = param.getSkuPrice().split("_");
            if (split.length == 2) {
                // 区间
                rangeQueryBuilder.gte(split[0]).lte(split[1]);
            } else if (split.length == 1) {
                if (param.getSkuPrice().startsWith("_")) {
                    rangeQueryBuilder.lte(split[0]);
                }
                if (param.getSkuPrice().endsWith("_")){
                    rangeQueryBuilder.gte(split[0]);
                }
            }
            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        // 把前面的所有 Query 条件都拿来，放到总 searchSourceBuilder 里
        searchSourceBuilder.query(boolQueryBuilder);

        /**
         * 2）排序，分页，高亮
         */
        // 2.1 排序
        if (!StringUtils.isEmpty(param.getSort())) {
            // sort=saleCount_asc/desc
            String sort = param.getSort();
            String[] s = sort.split("_");
            SortOrder order = s[1].equalsIgnoreCase("asc") ? SortOrder.ASC : SortOrder.DESC;
            searchSourceBuilder.sort(s[0], order);
        }
        // 2.2 分页
        // from = (pageNum-1)*size
        searchSourceBuilder.from((param.getPageNum()-1)*EsConstant.PRODUCT_PAGESIZE);
        searchSourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        // 2.3 高亮
        // 只有Keyword存在时才需要高亮，Keyword是用来模糊匹配的
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            searchSourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 3）聚合 aggregation（聚合的意思是将query的结果聚合起来处理，大多数起到分组的效果）
         * AggregationBuilders 是一个快速构造 AggregationBuilder 的工具类
         */
        // 3.1 品牌聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        // 品牌聚合的子聚合
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        searchSourceBuilder.aggregation(brand_agg);

        // 3.2 分类聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        searchSourceBuilder.aggregation(catalog_agg);

        // 3.3 属性聚合
        // 这里有两重子聚合
        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        // 聚合分析出当前所有商品具有多少种attr_id
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(10);
        // 聚合分析出当前attr_id对应的attrName
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 聚合分析出当前attr_id对应的所有可选attrValue
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attr_agg.subAggregation(attr_id_agg);
        searchSourceBuilder.aggregation(attr_agg);

        // 打印 DSL
        System.out.println("构建的DSL = " + searchSourceBuilder.toString());

        // 指定要查的 索引 和 searchSourceBuilder
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 封装检索结果
     */
    private SearchResult buildSearchResult(SearchResponse searchResponse, SearchParam param) {

        SearchResult result = new SearchResult();

        // 1. 返回所有查询到的商品
        SearchHits hits = searchResponse.getHits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits()!=null && hits.getHits().length>0) {
            for (SearchHit hit : hits.getHits()) {
                String sourceAsString = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String highLightSkuTitle = skuTitle.getFragments()[0].string();
                    esModel.setSkuTitle(highLightSkuTitle);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        // 2. 当前所有商品涉及到的所有分类信息
        List<SearchResult.CatalogVo> catalogVoList = new ArrayList<>();
        ParsedLongTerms catalogAgg = searchResponse.getAggregations().get("catalog_agg");
        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            // 得到分类Id
            String keyAsString = bucket.getKeyAsString();
            catalogVo.setCatalogId(Long.parseLong(keyAsString));

            // 得到分类名，每个分类id的bucket只对应一个分类名，所以可以直接取子聚合的0号bucket
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            String catalogName = catalogNameAgg.getBuckets().get(0).getKeyAsString();
            catalogVo.setCatalogName(catalogName);

            catalogVoList.add(catalogVo);
        }
        result.setCatalogs(catalogVoList);

        // 3. 当前所有商品涉及到的所有品牌信息
        List<SearchResult.BrandVo> brandVoList = new ArrayList<>();
        ParsedLongTerms brandAgg = searchResponse.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            // 得到品牌Id
            long brandId = bucket.getKeyAsNumber().longValue();
            brandVo.setBrandId(brandId);
            // 得到品牌图片，每个品牌id的bucket只对应一个图片，所以可以直接取子聚合的0号bucket
            ParsedStringTerms brandImgAgg = bucket.getAggregations().get("brand_img_agg");
            String brandImg = brandImgAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandImg(brandImg);
            // 得到品牌名，每个品牌id的bucket只对应一个品牌名，所以可以直接取子聚合的0号bucket
            ParsedStringTerms brandNameAgg = bucket.getAggregations().get("brand_name_agg");
            String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
            brandVo.setBrandName(brandName);

            brandVoList.add(brandVo);
        }
        result.setBrands(brandVoList);

        // 4. 当前所有商品涉及到的所有属性信息
        List<SearchResult.AttrVo> attrVoList = new ArrayList<>();

        ParsedNested attrAgg = searchResponse.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            // attr id 属性id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);

            // attr name 属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attr_name_agg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);

            // attr value 属性的所有可取值
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValueList = attrValueAgg.getBuckets().stream().map(item -> {
                String keyAsString = item.getKeyAsString();
                return keyAsString;
            }).collect(Collectors.toList());
            attrVo.setAttrValue(attrValueList);

            attrVoList.add(attrVo);
        }
        result.setAttrs(attrVoList);

        // 5.分页信息-当前页码
        result.setPageNum(param.getPageNum());
        // 5.分页信息-总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);
        // 5.分页信息-总页码
        int totalPages = (int) (total%EsConstant.PRODUCT_PAGESIZE==0 ? total/EsConstant.PRODUCT_PAGESIZE : (total/EsConstant.PRODUCT_PAGESIZE+1));
        result.setTotalPages(totalPages);
        // 5.分页信息-导航页码
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 6. 构建面包屑导航功能
        if (param.getAttrs()!=null && param.getAttrs().size()>0) {
            List<SearchResult.NavVo> navVoList = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                // attrs = 2_5寸:6寸
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                result.getAttrIds().add(Long.parseLong(s[0]));

                // 这里获得属性名应该可以直接用ES聚合的结果，因为这里有属性id了，只是想多要一个属性值
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo responseVo = r.getData("attr", new TypeReference<AttrResponseVo>(){});
                    navVo.setNavName(responseVo.getAttrName());
                }else {
                    navVo.setNavName(s[0]);
                }
                // 取消了这个面包屑之后要跳转到哪个地方。 1.可以将请求地址里的url里面的当前条件置空
                // &attrs=15_海思（Hisilicon） queryString不会带?号
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.friendmall.com/list.html?"+replace);
                return navVo;
            }).collect(Collectors.toList());
            result.setNavs(navVoList);
        }

        // 6. 将品牌上到面包屑
        if (param.getBrandId()!=null && param.getBrandId().size()>0) {
            List<SearchResult.NavVo> navs = result.getNavs();

            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            // 远程查询所有品牌
            R r = productFeignService.brandsInfo(param.getBrandId());
            if (r.getCode()==0) {
                // 这里TypeReference的泛型如果不用同一种数据类型(比如这里中的BrandVo是简化的BrandEntity)，那起码要保证成员名相同，fastJSON才能逆转过来
                List<BrandVo> brand = r.getData("brand", new TypeReference<List<BrandVo>>(){});
                StringBuffer stringBuffer = new StringBuffer();
                String replace = "";
                for (BrandVo brandVo : brand) {
                    stringBuffer.append(brandVo.getName() + ";");
                    replace = replaceQueryString(param, brandVo.getBrandId().toString()+"", "brandId");
                }
                navVo.setNavValue(stringBuffer.toString());
                navVo.setLink("http://search.friendmall.com/list.html?"+replace);
            }
            navs.add(navVo);
        }

        //TODO 将分类上到面包屑，可以完全同上，但分类不需要导航取消，所以可以不替换url地址

        return result;
    }

    private static String replaceQueryString(SearchParam param, String value, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        return replace;
    }
}
