package com.lmax.disruptor;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.junit.Test;

import java.util.concurrent.ThreadFactory;

import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

public class SingleProducerSequencerTest {
    @Test
    public void shouldNotUpdateCursorDuringHasAvailableCapacity() throws Exception {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(16, new BusySpinWaitStrategy());

        for (int i = 0; i < 32; i++) {
            long next = sequencer.next();
            assertThat(sequencer.cursor.get(), not(next));

            sequencer.hasAvailableCapacity(13);
            assertThat(sequencer.cursor.get(), not(next));

            sequencer.publish(next);
        }
    }


    @Test
    public  void abc() {

        /**

         * Create a new Disruptor.

         * @param eventFactory 事件对象的数据

         * @param ringBufferSize 数组大小，必须是2^n

         * @param threadFactory 线程工厂

         * @param producerType 生产者策略。ProducerType.SINGLE和ProducerType.MULTI 单个生产者还是多个生产者.

         * @param waitStrategy 等待策略。用来平衡事件发布者和事件处理者之间的处理效率。提供了八种策略。默认是BlockingWaitStrategy

         */

        //初始化的逻辑大概是创建根据ProducerType初始化创造SingleProducerSequencer或MultiProducerSequencer。

        //初始化Ringbuffer的时候会根据buffsiz把事件对象放入entries数组。

        Disruptor<TradeBO> disruptor = new Disruptor<>(TradeBO::new, 2,

                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {

                        Thread thread = new Thread(r);

                        thread.setName("实战单线程生产者");

                        return thread;

                    }
                }, ProducerType.SINGLE, new BlockingWaitStrategy());

        //关联事件处理者。初始化BatchEventProcessor。把事件处理者加入gating sequence

        disruptor.handleEventsWith(new ConsumerA());

        disruptor.handleEventsWith(new ConsumerB());

        //启动消费者线程。BatchEventProcessor间接实现了Runnable。所以这一步就是启动线程。如果事件发布太快，消费太慢会根据不同的waitstrategy等待。

        disruptor.start();

        //发布事件

        for (int i = 1; i < 10; i++) {

            int finalI = i;

            //初始化了EventTranslator。意思就是给最开始初始化的对象赋值

            EventTranslator eventTranslator = new EventTranslator<TradeBO>() {
                @Override
                public void translateTo(TradeBO event, long sequence) {

                    event.setId(finalI);

                    event.setPrice((double) finalI);

                }
            };

            //发布首先要申请序列，如果申请不到会自旋。

            disruptor.publishEvent(eventTranslator);

        }

        disruptor.shutdown();

    }



    class ConsumerB implements EventHandler<TradeBO> {

        @Override

        public void onEvent(TradeBO event, long sequence,

                            boolean endOfBatch) throws Exception {

            System.out.println("ConsumerB id=" + event.getId() + "  price=" + event.getPrice()+ "  seq=" + sequence);

        }

    }

    class ConsumerA implements EventHandler<TradeBO> {

        @Override

        public void onEvent(TradeBO event, long sequence,

                            boolean endOfBatch) throws Exception {

            System.out.println("ConsumerA id=" + event.getId() + "  price=" + event.getPrice() + "  seq=" + sequence);

        }

    }


    public class TradeBO {

        private Integer id;

        private Double price;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

    }
}