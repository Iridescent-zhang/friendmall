package com.aeterna.friendmall.search.thread;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : lczhang
 * @version : 1.0
 * @Project : friendmall
 * @Package : com.aeterna.friendmall.search.thread
 * @ClassName : .java
 * @createTime : 2024/7/26 12:48
 * @Email : lczhang93@gmail.com
 * @Website : https://iridescent-zhang.github.io
 * @Description :
 */
public class ThreadTest {

    // 当前系统中线程池最好只有一两个，每个异步任务都提交给线程池让他分配线程去执行
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start....");
        /**
         * CompletableFuture
         * 用来执行异步任务，并可以对任务进行异步编排，类似 vue 里面的 promise，执行完后then执行再接着then，
         *          挨个执行任务，编排任务的执行顺序。并返回一个 CompletableFuture，所以可以链式调用then
         *
         * CompletableFuture.
         *      runAsync(Runnable runnable,Executor executor)  没返回值
         *      supplyAsync(Supplier<U> supplier,Executor executor)  有返回值
         *      Supplier 和 Runnable 都是 @FunctionalInterface函数式接口，所以可以用lambda表达式写一个函数，这个函数需要实现这个接口里面的函数
         *                               比如 Runnable 的 void run();，Supplier 的 T get();，
         *                               BiConsumer 的 void accept(T t, U u);，Function 的 R apply(T t);
         *                               BiFunction<T, U, R> 的 R apply(T t, U u);
         */
//        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//        }, executor);

        /**
         * 计算完成时回调 whenComplete，在上一个任务完成后做一件事
         * whenComplete 可以处理正常和异常的计算结果，exceptionally 处理异常情况。
         *     whenComplete 和 whenCompleteAsync 的区别：
         *     whenComplete：是执行当前任务的线程继续执行 whenComplete 的任务。
         *     whenCompleteAsync：是把 whenCompleteAsync 这个任务继续提交给线程池
         *
         * 方法不以 Async 结尾，意味着 Action 使用相同的线程执行，而 Async 可能会使用其他线程
         *     执行（如果是使用相同的线程池，也可能会被同一个线程选中执行）
         */
          // whenComplete 方法成功完成后的感知
//        CompletableFuture<Integer> supplyCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).whenComplete((result,exception)->{
//            // whenComplete 虽然能得到异常信息，但没法修改返回的异常数据，相当于只是一个监听器，exceptionally可以感知异常并返回默认值
//            // whenComplete(BiConsumer<? super T, ? super Throwable> action)   void accept(T t, U u);
//            System.out.println("异步任务成功完成了...结果是："+result+";异常是"+exception);
//        }).exceptionally(exception->{
//            // 这个返回是的第一个任务里面的i，相当于结果出现异常了，我用这个默认值去代替那个结果
//            // Function<Throwable, ? extends T>   R apply(T t);
//            return 10;
//        });

        // handle 方法执行完就进行的处理（方法可能成功可能失败）
//        CompletableFuture<Integer> supplyCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).handle((result, throwable)->{
//            //BiFunction<T, U, R>    R apply(T t, U u);
              // 方法成功就把result*2 否则返回0
//            if (result!=null) {
//                return result*2;
//            }
//            if (throwable==null) {
//                return 0;
//            }
//            return 0;
//        });

        /**
         * 线程串行化
         * 1. thenRunAsync 不能感知上一步的结果，且无返回值
         *         .thenRunAsync(()->{
         *             System.out.println("任务2启动了...");
         *         },executor)
         * 2. thenAcceptAsync 能接收上一步结果，但无返回值
         *         .thenAcceptAsync(result->{
         *             // thenAcceptAsync(Consumer<? super T> action, Executor executor)  void accept(T t);
         *             System.out.println("任务2启动了..."+result);
         *         })
         * 3. thenApplyAsync 能接收上一步结果，并且有返回值
         *         .thenApplyAsync(result -> {
         *             //thenApplyAsync(Function<? super T,? extends U> fn, Executor executor)   R apply(T t);
         *             System.out.println("任务2启动了..." + result);
         *             return "Hello " + result;
         *         }, executor)
         */

        /**
         * 两个任务的合并，两个任务完成后执行第三个任务
         * 1. runAfterBothAsync 不感知两个任务的结果，只执行第三个任务，且无返回值
         *         .runAfterBothAsync(future02, ()->{
         *             // void run();
         *             System.out.println("任务3开始...");
         *         }, executor)
         * 2. thenAcceptBothAsync 接收两个任务的返回值，执行第三个任务，无返回值
         *         .thenAcceptBothAsync(future02, (f1,f2)->{
         *             // accept(T t, U u)
         *             System.out.println("任务3开始...之前的结果为："+f1+"-->"+f2);
         *         }, executor)
         * 3. thenCombineAsync 可以接收两个任务的返回值，执行第三个任务，并且有返回值
         *         .thenCombineAsync(future02, (f1, f2) -> {
         *             // R apply(T t, U u);
         *             return "之前的两个结果：" + f1 + "-->" + f2 + "-->Hello";
         *         }, executor)
         */
//        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("任务1结束：" );
//            return i;
//        }, executor);
//        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务2线程：" + Thread.currentThread().getId());
//            try {
//                Thread.sleep(3000);  // 让future1 future2明显地先后完成，以验证 acceptEitherAsync 等
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            System.out.println("任务2结束：" );
//            return "Hello";
//        }, executor);
        // 任务3是任务1、2结束后开始
//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            // R apply(T t, U u);
//            return "之前的两个结果：" + f1 + "-->" + f2 + "-->Hello";
//        }, executor);

        /**
         * 两个任务下只要有一个 future 完成就执行任务3
         * runAfterEither：两个任务有一个执行完成，不获取future的结果，也没有返回值。
         *         .runAfterEitherAsync(future02, () -> {
         *             // void run();
         *             System.out.println("任务3开始...");
         *         }, executor)
         * acceptEither：两个任务有一个执行完成，能够获取它的返回值，但没有新的返回值。
         *         .acceptEitherAsync(future02, (res)->{
         *             // void accept(T t);
         *             System.out.println("任务3开始..."+res);
         *         }, executor)
         * applyToEither：两个任务有一个执行完成，能够获取它的返回值，并有新的返回值。
         *         .applyToEitherAsync(future02, (res)->{
         *             // R apply(T t);
         *             System.out.println("任务3开始..."+res);
         *             return res.toString()+"->哈哈";
         *         }, executor)
         *
         */
//        CompletableFuture<Object> future = future01.applyToEitherAsync(future02, (res)->{
//            // R apply(T t);
//            System.out.println("任务3开始..."+res);
//            return res.toString()+"->哈哈";
//        }, executor);

        /**
         * 多任务组合
         * allOf：等待所有任务完成
         *      CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
         * anyOf：只要有一个任务完成就行
         *
         * allOf.get();  // 等待全部任务做完
         * allOf.join();  // 将线程都插入进来，等待异步任务执行完成
         */
        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的图片信息...");
            return "hello.jpg";
        }, executor);
        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品的属性...");
            return "黑色+256G";
        }, executor);
        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("查询商品的介绍...");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "华为";
        }, executor);

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
//        allOf.get();  // 等待全部任务做完
        anyOf.join();  // 将线程都插入进来，等待异步任务执行完成

//        System.out.println("main....end...."+futureImg.get()+"=>"+futureAttr.get()+"=>"+futureDesc.get());  // allOf才能用，anyOf都不知道是哪个成功了
        System.out.println("main....end...."+anyOf.get());
    }

    public void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start....");
        /**
         * 获得线程的方法：
         * 1）、继承Thread
         *         Thread01 thread = new Thread01();
         *         thread.start();  // 启动线程
         * 2）、实现Runnable 接口
         *         Runnable01 runnable = new Runnable01();
         *         new Thread(runnable).start();  // 启动线程
         * 3）、实现Callable 接口+ FutureTask （可以拿到返回结果，可以处理异常）
         *         FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
         *         // new thread()里面要放一个Runnable，而FutureTask也继承了Runnable，因此FutureTask也是可以支持Thread线程运行的
         *         new Thread(futureTask).start();
         *         // 阻塞等待：这样会等待整个线程执行完成，并获取返回结果。 如果没有这个，主进程是不会等待线程完成的，是异步完成的
         *         Integer result = futureTask.get();
         * 4）、线程池【就是 ExecutorService，ExecutorService extends Executor】
         *         直接给线程池提交任务执行，执行时 submit和execute 区别在于前者能够获取任务的返回值，execute只执行任务，没有返回值
         *         service.execute(new Runnable01());
         *       1) 创建
         *          1. Executors 工具类
         *          2. new ThreadPoolExecutor()
         *
         *      FutureTask<V> implements RunnableFuture<V> extends Runnable, Future<V>
         *      Future<V> 可以获取到异步结果
         *      CompletableFuture<T> implements Future<T>, CompletionStage<T>
         *      所以 CompletableFuture<T> 也能获取异步任务的结果
         *
         * 区别：
         *     1、2不能得到返回值，3可以得到返回值
         *     1、2、3都不能控制资源
         *     4可以控制整体资源，整个系统性能稳定。
         */
        // 以后在业务代码里，类似如下的前三种启动线程的方式都不用，防止高并发情况下资源耗尽。
        // 【应该将所有的多线程异步任务都交给线程池执行】
        // new Thread(()-> System.out.println("hello")).start();

        /**
         * 七大参数
         * int corePoolSize 核心线程数[一直存在除非设置allowCoreThreadTimeOut]，创建好后就准备了这么多线程
         *         比如 5个 Thread thread = new Thread();   分配任务后 thread.start();
         * int maximumPoolSize 最大线程数量，当核心线程不够用且工作队列也满了，逐渐增加线程，但不超过这个数，起到控制资源的效果
         * long keepAliveTime 存活时间，如果当前线程数大于core核心线程数，并且这些线程空闲时间超过了设置的keepAliveTime，这些线程便会被释放
         * TimeUnit unit 时间单位 ，keepAliveTime
         * BlockingQueue<Runnable> workQueue 阻塞队列，如果任务很多，就会将多出来的任务放到workQueue工作队列里面，
         *                                   只要有线程空闲，就回去工作队列里面取出新的任务执行
         * ThreadFactory threadFactory 线程的创建工厂
         * RejectedExecutionHandler handle 如果工作队列满了，按照指定的拒绝策略拒绝执行任务
         *
         * 工作流程：
         *  1、线程池创建，准备好core 数量的核心线程，准备接受任务
         *  2、新的任务进来，用 core 准备好的空闲线程执行。
         *      (1) 、core 满了，就将再进来的任务放入阻塞队列中。空闲的core 就会自己去阻塞队列获取任务执行
         *      (2) 、阻塞队列满了，就直接开新线程执行，最大只能开到max 指定的数量
         *      (3) 、max 都执行好了。Max-core 数量空闲的线程会在 keepAliveTime 指定的时间后自动销毁。最终保持到 core 大小
         *      (4) 、如果线程数开到了 max 的数量，队列也满了，还有新任务进来，就会使用 reject 指定的拒绝策略进行处理
         *  3、所有的线程创建都是由指定的factory 创建的
         *
         * 细节：
         *      new LinkedBlockingDeque<>() 默认是Integer.MAX_VALUE，阻塞队列会太大了，这玩意也是占内存的
         *      面试例子：一个线程池core 7； max 20 ，queue：50，100 并发进来怎么分配的；
         *      先有7个能直接得到执行，接下来50个进入队列排队，在多开13个继续执行。现在70个被安排上了。剩下30个默认拒绝策略。
         *          如果不想直接拒绝，使用策略CallerRunsPolicy，新任务(Runnable)进来它会同步执行新任务里面的run方法
         *          为什么说是同步执行，因为没有在另开的线程里面执行而是直接执行了，只有new Thread再start的才是异步调用
         */
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        /**
         *  newCachedThreadPool
         *       core=0，max=Integer.MAX_VALUE 所有线程都可回收，灵活创建
         *
         *  newFixedThreadPool
         *       core==max，线程数固定，超出的线程会在队列中等待。
         *
         *  newScheduledThreadPool
         *       提交任务时可指定多长时间后执行，专门用于做定时任务的线程池
         *
         *  newSingleThreadExecutor
         *       单线程的线程池，它只会用唯一的工作线程来执行任务，保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行。
         */
//        Executors.newCachedThreadPool();
//        Executors.newFixedThreadPool();
//        Executors.newScheduledThreadPool();
//        Executors.newSingleThreadExecutor();
    }

    // 1）、继承Thread
    public static class Thread01 extends Thread{
        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果："+i);
        }
    }

    // 2）、实现Runnable 接口
    public static class Runnable01 implements Runnable{
        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果："+i);
        }
    }

    // 3）、实现Callable 接口+ FutureTask （可以拿到返回结果，可以处理异常）
    public static class Callable01 implements Callable<Integer> {

        /**
         * Computes a result, or throws an exception if unable to do so.
         *
         * @return computed result
         * @throws Exception if unable to compute a result
         */
        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i = 10/2;
            System.out.println("运行结果："+i);
            return i;
        }
    }
}
