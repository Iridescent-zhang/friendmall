package com.aeterna.friendmall.product.service.impl;

import com.aeterna.friendmall.product.service.CategoryBrandRelationService;
import com.aeterna.friendmall.product.vo.Catalog2Vo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.aeterna.common.utils.PageUtils;
import com.aeterna.common.utils.Query;

import com.aeterna.friendmall.product.dao.CategoryDao;
import com.aeterna.friendmall.product.entity.CategoryEntity;
import com.aeterna.friendmall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 1. 查出所有分类
        // baseMapper 就是 CategoryDao，也可以选择把CategoryDao注入进来，一模一样的
        List<CategoryEntity> categoryEntityList = baseMapper.selectList(null);

        // 2. 组装成父子的树形结构
        List<CategoryEntity> level1Menus = categoryEntityList.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu)->{  // 在每个menu被collect之前先设置它的子类，设置完返回
            menu.setChildren(getChildrens(menu, categoryEntityList));
            return menu;
        }).sorted((menu1,menu2)->{  // 将menu根据sort属性进行排序放到List中，List是有序集合
            return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * 根据当前类别查找它所有的子类别并返回，但要递归地进行
     * @param root 当前类
     * @param all 所有类
     * @return 当前类的排好序的子类构成的集合
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all){

        List<CategoryEntity> children = all.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map((categoryEntity)->{  // 能进到这里的是已经被过滤过的，也就是root的第一级子目录，然后为每个这个子目录接着去设置它的子，并把它排序和收集最后返回
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }

    @Override
    public void removeMenuByids(List<Long> list) {
        // TODO 1. 检查要删除的菜单是否在其他地方被引用

        // 默认是直接删除，通过配置实现逻辑删除
        baseMapper.deleteBatchIds(list);

    }

    /**
     * [2, 25, 225]
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();

        List<Long> ParentPath = findParentPath(catelogId, path);

        Collections.reverse(ParentPath);

        return ParentPath.toArray(new Long[ParentPath.size()]);
    }

    /**
     * 级联更新所有关联的表
     * 1. @CacheEvict：失效模式的使用
     * 2. @Caching：将多个缓存操作组合在一起
     * 第二种方法可以删除一个缓存区中所有 key ：@CacheEvict(value = "category", allEntries = true)，体现缓存分区的好处
     * 3. 约定 同一类型的数据放在同一缓存分区（注意这个只是spring这么分，方便管理），到时候好删，以缓存分区名作为key的前缀
     */
//    @Caching(evict = {
//            @CacheEvict(value = "category", key = "'getLevel1Categorys'"),
//            @CacheEvict(value = "category", key = "'getCatalogJson'")
//    })  // 组合多个操作的注解
    @CacheEvict(value = "category", allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

        // 1. 双写模式：写完数据库就去更新缓存
        // 2. 失效模式：数据库改完就删掉缓存，等待下次主动查询进行更新
        // redis.del("CatalogJSON")
    }

    /**
     * 1. @Cacheable() 代表当前方法的结果需要进行缓存：如果缓存中有结果，直接缓存中拿；如果缓存中没有，则调用方法，并且最后将结果放入缓存
     *   每个缓存的数据都要指定要放到哪个缓存分区
     *      【通过分区的名字指定，这个分区是逻辑分区，只是理论上这么分，实际上都是存到redis，并且我们建议通过业务类型分，可以放多份】
     * 2. 默认行为：
     *      1. 缓存中有则不调用方法
     *      2. key自动生成，格式为：缓存的名字：：SimpleKey [] ，如category::SimpleKey []
     *      3. vslue值默认使用jdk序列化机制，将序列化后的数据存到redis
     *      4. 默认ttl时间：-1 永不过时
     * 3. 最好自定义：
     *      1. 指定缓存使用的key， 用key属性指定，接收的是SpEL，动态指定，如#root表示这个缓存数据，#root.method.name为生成此数据的方法的名字
     *              https://docs.spring.io/spring-framework/docs/5.3.37/reference/html/integration.html#cache-spel-context
     *      2. 指定缓存数据的存活时间， 配置文件中修改 ttl：spring.cache.redis.time-to-live=3600000 (ms)
     *      3. 将数据value保存为Json格式:
     * 4. Spring-Cache的不足：
     *      1）读模式：
     *        缓存穿透：查询不存在的，解决：缓存null数据 spring.cache.redis.cache-null-values=true
     *        缓存击穿：查询过期热点，解决：加锁  解决了？没有，默认没加锁，通过 @Cacheable sync = true 加锁解决，这里是本地锁
     *        缓存雪崩：大量key同时过期，解决: 加过期时间，实际上用随机时间反而可能弄巧成拙，由于我们数据生效时间不同，在时间轴上就是不同，因此只要加上过期时间就可以了
     *      2）写模式：（缓存与数据库最终一致）  最终SpringCache没有考虑写模式，毕竟普通数据只要有过期时间，写模式也完全没问题
     *        1. 读写加锁
     *        2. 引入中间件 Canal（阿里的），感知数据库修改去更新缓存
     *        3. 写多的就不加锁，直接查数据库
     *    原理：
     *        CacheManager(RedisCacheManager)->Cache(RedisCache)->Cache 负责缓存读写
     *    总结：
     *        常规数据（读多写少，即时性、一致性要求不高的数据）完全可以使用Spring-Cache解决
     *        特殊数据：特殊设计，如Canal、公平锁、读写锁
     *
     */
    @Cacheable(value = {"category"}, key = "#root.method.name", sync = true)  // 'level1Categorys'
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        System.out.println("getLevel1Categorys()调用了...");
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return  categoryEntities;
    }

    //TODO：redis 产生堆外内存溢出：OutOfDirectMemoryError

    @Cacheable(value = {"category"}, key = "#root.methodName")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {

        System.out.println("查询了一次数据库...");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);

        // 2. 封装数据   Collectors.toMap这个写法是直接封装为Map，key 和 value 要分别用lambda表达式实现
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 2.1 查每个一级分类的二级分类
            List<CategoryEntity> category2Entities = getParentCid(selectList, v.getCatId());
            // 2.2 封装为二级分类Vo
            List<Catalog2Vo> catalog2Vos = null;
            if (category2Entities != null) {
                catalog2Vos = category2Entities.stream().map(category2Entity -> {

                    // 为这个二级分类找它的三级分类并封装为Vo3List
                    List<CategoryEntity> category3Entities = getParentCid(selectList, category2Entity.getCatId());
                    List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                    if (category3Entities != null) {
                        catalog3Vos = category3Entities.stream().map(category3Entity -> {
                            // 直接用全参构造器构建
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(category2Entity.getCatId().toString(), category3Entity.getCatId().toString(), category3Entity.getName().toString());

                            return catalog3Vo;
                        }).collect(Collectors.toList());
                    }

                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), catalog3Vos, category2Entity.getCatId().toString(), category2Entity.getName().toString());

                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        return parent_cid;
    }

    /**
     * 1. springboot2.0后默认使用 lettuce 作为操作redi的客户端，它使用 netty 进行网络通信
     *     lettuce 的 bug 导致堆外内存溢出，netty如果没有指定堆外内存，默认设置与服务相同 -Xmx100m；
     *     可以通过 -Dio.netty.maxDirectMemory 设置，但不能只设置这个调大堆外内存，lettuce 没有及时释放失败的连接，迟早会满
     *     解决方案：1. 升级 lettuce 客户端   2. 切换使用客户端 jedis
     *
     *  lettuce 和 jedis 都是操作 redis 的最底层客户端，spring 对其封装为 RedisTemplate
     */
    public Map<String, List<Catalog2Vo>> getCatalogJson2(){
        /**
         * 1.空结果缓存：解决缓存穿透（查一个不存在的数据全穿透缓存去查数据库）
         * 2.设置随机过期时间：解决缓存雪崩（很多缓存数据同时失效，查询全到db去了）
         * 3.加锁：解决缓存击穿（热点数据失效了，高并发请求全到数据库）
         */

        // 1. 加入缓存逻辑， 约定缓存中存放的值value是json字符串
        // 因为JSON跨语言、跨平台兼容
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            // 缓存中没有，则查询数据库并放入缓存
            System.out.println("缓存不命中...查询数据库...");
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedisLock();

            return catalogJsonFromDb;
        }

        /**
         * 因此，从缓存中查到的json字符串也要逆转为可用的对象类型：【序列化与反序列化】
         * 如果 FastJSON 要将json转为复杂类型，要写成 TypeReference
         * TypeReference 是 protected 修饰的，所以要以(也可匿名)内部类的方式来构造
         *      TypeReference<Map<String, List<Catalog2Vo>>> typeReference = new TypeReference<Map<String, List<Catalog2Vo>>>(){};
         *
         */
        System.out.println("缓存命中...直接返回");
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>(){});

        return result;
    }

    /**
     * 如何保证缓存数据和数据库保持一致
     * 缓存数据一致性：
     * 1. 双写模式：写完数据库就去更新缓存
     *      读到的最新数据有延迟性：一定会有一段时间，缓存数据不是最新，以及会有脏读问题（两个线程并发写，一个线程两个操作夹在另一个线程中间了），
     *      脏读可通过将写数据库和写缓存整体加锁，作为原子行为。最终缓存过期后总能读到最新数据(最终一致性)
     * 2. 失效模式：数据库改完就删掉缓存，等待下次主动查询进行更新
     *      同样会有脏数据问题，通过读写锁解决并发读写
     *
     * 我们系统的一致性解决方案：使用 失效模式
     *      1. 缓存的所有数据都有过期时间，保证最终一致性
     *      2. 读写数据加分布式的读写锁
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        // 锁名牵扯到锁的粒度，粒度越细，锁的资源越少，运行的速度越快。如果多个服务用同一把锁，那这把锁粒度很粗，锁了很多资源
        // 约定：具体缓存的某个数据，如11号商品 product-11-lock product-12-lock，如果对所有商品用同一把product-lock是很蠢的，因为访问11的还要等12的锁释放吗？
        RLock lock = redisson.getLock("CatalogJsonLock");
        lock.lock();

        Map<String, List<Catalog2Vo>> dataFromDb;
        try {
            dataFromDb = getDataFromDb();
        }finally {
            lock.unlock();
        }

        return dataFromDb;

    }

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {

        // 1. 占分布式锁 去redis占坑
        // 2. 占锁和设置过期时间必须是同步的，需要是一个原子操作，是一个整体，不然断电等极端情况还是会死锁
        // 3. 使用随机 UUID ，防止自己业务超时导致锁过期，删锁的时候删了别人正在持有的锁
        // 关键点在于：加锁、删锁的原子性
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        if (lock) {
            System.out.println("成功获取分布式锁...");
            // 占坑(加锁)成功...执行业务
            // 2. 错误示例：占锁和设置过期时间必须是同步的，需要是一个原子操作，是一个整体，不然断电等极端情况还是会死锁
//            stringRedisTemplate.expire("lock", 30, TimeUnit.SECONDS);
            Map<String, List<Catalog2Vo>> dataFromDb;
            try {
                dataFromDb = getDataFromDb();
            }finally {
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                Long execute = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                        , Arrays.asList("lock"), uuid);
            }
            // 完成业务后解锁
            /**
             * 这里出现和上面一样的问题，先从redis获取值然后匹配删除，这是两个操作，网络情况极端下可能还是删了别人的锁，相当于程序没锁住
             * 所以需要：获取值+对比成功删除=原子操作  -》 通过redis+lua脚本完成，毕竟脚本不能只执行一半吧
             */
//            String lockValue = stringRedisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                // 通过匹配保证自己只能删自己的锁
//                stringRedisTemplate.delete("lock");  // 删除锁
//            }
            return dataFromDb;
        }else {
            // 加锁失败...重试。同synchronized，一直监听直到别人释放
            System.out.println("获取分布式锁失败...等待重试");
            // 休眠100ms重试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
            }
            return getCatalogJsonFromDbWithRedisLock();  // 自旋
        }
    }

    // 从数据库查询并封装分类数据
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithLocalLock() {

         //只要是同一把锁，就能锁住需要这个锁的所有线程
         //1. synchronized (this)：springboot所有的组件在容器中都是单例的
         //TODO：本地锁synchronized、JUC(Lock)：只能锁住当前进程，对于分布式情况下，一个商品服务可能有多个实例，那每个本地所只能锁住自己那个实例，锁不住所有的服务，所以得用分布式锁

        synchronized (this) {
            /**
             * 压测优化：将多次查数据库变为一次
             */
            /**
             * 得到锁以后应该再去缓存中查一次，没有才继续查询，不然逻辑就不对了，锁白加，相当于还是排队查数据库
             */
            return getDataFromDb();
        }
    }

    // 纯业务
    private Map<String, List<Catalog2Vo>> getDataFromDb() {
        String catalogJSON = stringRedisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJSON)) {
            // 缓存有数据就返回结果
            Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catalog2Vo>>>(){});
            return result;
        }
        System.out.println("查询了一次数据库...");

        List<CategoryEntity> selectList = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getParentCid(selectList, 0L);

        // 2. 封装数据   Collectors.toMap这个写法是直接封装为Map，key 和 value 要分别用lambda表达式实现
        Map<String, List<Catalog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 2.1 查每个一级分类的二级分类
            List<CategoryEntity> category2Entities = getParentCid(selectList, v.getCatId());
            // 2.2 封装为二级分类Vo
            List<Catalog2Vo> catalog2Vos = null;
            if (category2Entities != null) {
                catalog2Vos = category2Entities.stream().map(category2Entity -> {

                    // 为这个二级分类找它的三级分类并封装为Vo3List
                    List<CategoryEntity> category3Entities = getParentCid(selectList, category2Entity.getCatId());
                    List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                    if (category3Entities != null) {
                        catalog3Vos = category3Entities.stream().map(category3Entity -> {
                            // 直接用全参构造器构建
                            Catalog2Vo.Catalog3Vo catalog3Vo = new Catalog2Vo.Catalog3Vo(category2Entity.getCatId().toString(), category3Entity.getCatId().toString(), category3Entity.getName().toString());

                            return catalog3Vo;
                        }).collect(Collectors.toList());
                    }

                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), catalog3Vos, category2Entity.getCatId().toString(), category2Entity.getName().toString());

                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        // 在同一把锁内 将查到的对象转为json放入缓存，保证查数据库、放缓存这整体是一个原子操作
        String jsonString = JSON.toJSONString(parent_cid);
        // 设置过期时间
        stringRedisTemplate.opsForValue().set("catalogJSON", jsonString, 1, TimeUnit.DAYS);
        return parent_cid;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        path.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0) {
            findParentPath(byId.getParentCid(), path);
        }
        return path;
    }
}