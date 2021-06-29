# Disruptor

## 简述
### 是什么

Disruptor是英国外汇交易公司LMAX开发的一个高性能队列；

### 为什么

要说为什么，首先要看一下现在Java内置的队列：

| 队列                  | 有界性 | 锁      | 数据结构   |
| --------------------- | ------ | ------- | ---------- |
| ArrayBlockingQueue    | 有     | 加锁    | arraylist  |
| LinkedBlockingQueue   | 可选   | 加锁    | linkedlist |
| ConcurrentLinkedQueue | 无     | 无锁CAS | linkedlist |
| LinkedTransferQueue   | 无     | 无锁CAS | linkedlist |
| PriorityBlockingQueue | 无     | 加锁    | heap       |
| DelayQueue            | 无     | 加锁    | heap       |

首先，无锁很难保证队列长度在确定的范围，所以基本无锁队列都是无界的；

生产使用中，防止生产速度远大于消费速度导致内存溢出的情况，需要选择有界队列；

为了减少垃圾回收对系统性能的影响，一般选择array/heap的数据结构；

综上，只能选择 ArrayBlockingQueue

#### ArrayBlockingQueue 的问题

加速导致性能损耗： CAS 比无锁慢一个数量级，多线程有锁比无锁要慢三个数量级；

伪共享导致的性能损耗；
- 缓存行 cache line (64bit)
- @Contended (1.8)

## Disruptor

### 环形数组

1. 避免垃圾回收，发挥缓存行机制的效能；
2. 元素定位2^n，位运算速度快，下标递增，没有index溢出问题；
3. 无锁设计；

### ringbuffer

1. 长度2^n，计算index：

> sequence & （array length－1） = array index

2. 没有尾指针，只维护了一个指向下一个可用位置的序号；
   
> 最初只是为了保证消息可靠传递，如果某个点出现了nak（拒绝应答信号），可以重发该节点到index之间的所有消息；
> 并且ringbuffer不删除数据，数据直接覆盖，ringbuffer本身并不控制是否需要重叠，决定是否重叠是生产者-消费者行为模式的一部分。

### 从Ringbuffer读取

ConsumerBarrier与消费者



### Disruptor对ringbuffer的访问控制策略


## 补充

### 等待策略

生产者的等待策略: 暂时只有休眠1ns。
```java
LockSupport.parkNanos(1);
```
消费者的等待策略
| 名称                        | 措施                      | 适用场景                                                                                    |
| --------------------------- | ------------------------- | ------------------------------------------------------------------------------------------- |
| BlockingWaitStrategy        | 加锁                      | CPU资源紧缺，吞吐量和延迟并不重要的场景                                                     |
| BusySpinWaitStrategy        | 自旋                      | 通过不断重试，减少切换线程导致的系统调用，而降低延迟。推荐在线程绑定到固定的CPU的场景下使用 |
| PhasedBackoffWaitStrategy   | 自旋 + yield + 自定义策略 | CPU资源紧缺，吞吐量和延迟并不重要的场景                                                     |
| SleepingWaitStrategy        | 自旋 + yield + sleep      | 性能和CPU资源之间有很好的折中。延迟不均匀                                                   |
| TimeoutBlockingWaitStrategy | 加锁，有超时限制          | CPU资源紧缺，吞吐量和延迟并不重要的场景                                                     |
| YieldingWaitStrategy        | 自旋 + yield + 自旋       | 性能和CPU资源之间有很好的折中。延迟比较均匀                                                 |

### 应用列举

#### Log4j 2
Log4j 2相对于Log4j 1最大的优势在于多线程并发场景下性能更优。该特性源自于Log4j 2的异步模式采用了Disruptor来处理。 在Log4j 2的配置文件中可以配置WaitStrategy，默认是Timeout策略。

## 总结

Disruptor通过精巧的无锁设计实现了在高并发情形下的高性能。

高并发场景可以借鉴Disruptor的设计，减少竞争的强度。设计思想可以扩展到分布式场景，通过无锁设计，来提升服务性能。

使用Disruptor比使用ArrayBlockingQueue略微复杂，为方便读者上手，增加代码样例。

代码实现的功能：每10ms向disruptor中插入一个元素，消费者读取数据，并打印到终端。详细逻辑请细读代码。

以下代码基于3.3.4版本的Disruptor包。

```java
/**
 * @description disruptor代码样例。每10ms向disruptor中插入一个元素，消费者读取数据，并打印到终端
 */
public class DisruptorMain
{
    public static void main(String[] args) throws Exception
    {
        // 队列中的元素
        class Element {

            private int value;

            public int get(){
                return value;
            }

            public void set(int value){
                this.value= value;
            }

        }

        // 生产者的线程工厂
        ThreadFactory threadFactory = new ThreadFactory(){
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "simpleThread");
            }
        };

        // RingBuffer生产工厂,初始化RingBuffer的时候使用
        EventFactory<Element> factory = new EventFactory<Element>() {
            @Override
            public Element newInstance() {
                return new Element();
            }
        };

        // 处理Event的handler
        EventHandler<Element> handler = new EventHandler<Element>(){
            @Override
            public void onEvent(Element element, long sequence, boolean endOfBatch)
            {
                System.out.println("Element: " + element.get());
            }
        };

        // 阻塞策略
        BlockingWaitStrategy strategy = new BlockingWaitStrategy();

        // 指定RingBuffer的大小
        int bufferSize = 16;

        // 创建disruptor，采用单生产者模式
        Disruptor<Element> disruptor = new Disruptor(factory, bufferSize, threadFactory, ProducerType.SINGLE, strategy);

        // 设置EventHandler
        disruptor.handleEventsWith(handler);

        // 启动disruptor的线程
        disruptor.start();

        RingBuffer<Element> ringBuffer = disruptor.getRingBuffer();

        for (int l = 0; true; l++)
        {
            // 获取下一个可用位置的下标
            long sequence = ringBuffer.next();  
            try
            {
                // 返回可用位置的元素
                Element event = ringBuffer.get(sequence); 
                // 设置该位置元素的值
                event.set(l); 
            }
            finally
            {
                ringBuffer.publish(sequence);
            }
            Thread.sleep(10);
        }
    }
}
```




