package com.yacoding.socket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author yaCoding
 * @create 2022-06-23 下午 2:58
 */

public class NIO {

    static boolean stop = false;

    public static void main(String[] args) throws Exception{

        int connectionNum  = 0;
        int port = 8888;
        ExecutorService service = Executors.newCachedThreadPool();
        //创建了一个服务端ssc，并开启一个新的事件选择器，监听它的OP_ACCEPT事件。
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.socket().bind(new InetSocketAddress("localhost",port));
        Selector selector = Selector.open();

        //共有4种事件类型。
        // 分别是新连接事件（OP_ACCEPT）、连接就绪事件（OP_CONNECT）、读就绪事件（OP_READ）、写就绪事件（OP_WRITE）。
        // 任何网络和文件操作，都可以抽象成这四个事件
        //SelectionKey.OP_ACCEPT = 16 ssc.validOps()=16

        ssc.register(selector, ssc.validOps());
        while (!stop) {
            if (10 == connectionNum) {
                stop = true;
            }
            //在while循环里，使用select函数，阻塞在主线程里。
            // 所谓阻塞，就是操作系统不再分配CPU事件片到当前线程中，所以select函数是几乎不占用任何系统资源的
            int num = selector.select();
            if (num == 0) {
                continue;
            }
            //一旦有新的事件到达，比如有新的连接到来，主线程就能够被调度到，程序就能够向下执行。
            // 这时候，就能够根据订阅的事件通知，持续获取订阅的事件。
            //由于注册到selector的连接和事件可能会有多个，所以这些事件也会有多个
            //使用安全的迭代器循环进行处理，在处理完毕之后，将它删除。
            //如果事件不删除的话，或者漏掉了某个事件的处理，会怎么样呢？
            // 后果还是比较严重的，由于事件总是存在，我们的程序会陷入无休无止的循环之中。
            Iterator<SelectionKey> events = selector.selectedKeys().iterator();

            while (events.hasNext()) {
                SelectionKey event = events.next();
                if (event.isAcceptable()) {
                    //NIO操作的对象是抽象的概念Channel，通过缓冲区进行数据交换
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    //订阅OP_READ事件，数据流读取
                    sc.register(selector, SelectionKey.OP_READ);
                    connectionNum++;
                } else if (event.isReadable()) {
                    try {
                        SocketChannel sc = (SocketChannel) event.channel();
                        //创建了一个1024字节的缓冲区，用于数据的读取
                        //如果连接中的数据，大于1024字节怎么办？
                        //水平触发 (level-triggered) 称作LT模式。只要缓冲区有数据，事件就会一直发生
                        //边缘触发 (edge-triggered) 称作ET模式。缓冲区有数据，仅会触发一次。事件想要再次触发，必须先将fd中的数据读完才行
                        //java的NIO使用了LT模式，LT模式频繁环唤醒线程，效率相比较ET模式低，所以Netty使用JNI的方式，实现了ET模式，效率上更高一些
                        ByteBuffer buf = ByteBuffer.allocate(1024);
                        int size = sc.read(buf);
                        if (-1 == size) {
                            sc.close();
                        }
                        String result = new String(buf.array()).trim();
                        ByteBuffer wrap = ByteBuffer.wrap(("PONG:" + result).getBytes());
                        sc.write(wrap);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else if (event.isWritable()) {
                    SocketChannel sc = (SocketChannel) event.channel();
                }
                events.remove();
                    }
                }
        service.shutdown();
        ssc.close();
    }
}
