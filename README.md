# netty
##  Netty架构

> 在Netty中，Boss线程对应着对连接的处理和分派，相当于mainReactor；Work线程 对应着subReactor，使用多线程负责读写事件的分发和处理。

![image](https://user-images.githubusercontent.com/82166879/176911726-252c04e2-6f63-41a7-944b-d11755b1977e.png)
**EventLoop,EventLoopGroup**

EventLoop目的是为Channel处理IO操作，一个EventLoop可以为多个Channel服务,EventLoopGroup会包含多个EventLoop

**Channel**

请求连接通道

**ChannelHandler**

业务逻辑处理器

**ChannelHandlerContext**

业务逻辑处理器上下文

**ChannelPipeline**

用于保存业务逻辑处理器和业务逻辑处理器上下文
### 架构图说明

1. Netty 抽象出两组线程池：`BossGroup` 和 `WorkerGroup`，也可以叫做 `BossNioEventLoopGroup `和 `WorkerNioEventLoopGroup`。每个线程池中都有 `NioEventLoop` 线程。BossGroup 中的线程专门负责和客户端建立连接，WorkerGroup 中的线程专门负责处理连接上的读写。BossGroup 和 WorkerGroup 的类型都是 `NioEventLoopGroup`。
2. `NioEventLoopGroup` 相当于一个事件循环组，这个组中含有多个事件循环，每个事件循环就是一个 `NioEventLoop`。
3. `NioEventLoop` 表示一个不断循环的执行事件处理的线程，每个 `NioEventLoop` 都包含一个 `Selector`，用于监听注册在其上的 Socket 网络连接（`Channel`）
4. `NioEventLoopGroup` 可以含有多个线程，即可以含有多个 `NioEventLoop`
5. 每个 `BossNioEventLoop` 中循环执行以下三个步骤:
   1. **select**：轮询注册在其上的 `ServerSocketChannel` 的 accept 事件（OP_ACCEPT 事件）
   2. **processSelectedKeys**：处理 accept 事件，与客户端建立连接，生成一个 `NioSocketChannel`，并将其注册到某个 `WorkerNioEventLoop` 上的 `Selector` 上
   3. **runAllTasks**：再去以此循环处理任务队列中的其他任务
6. 每个 `WorkerNioEventLoop` 中循环执行以下三个步骤：
   1. **select**：轮询注册在其上的 `NioSocketChannel` 的 read/write 事件（OP_READ/OP_WRITE 事件）
   2. **processSelectedKeys**：在对应的 `NioSocketChannel` 上处理 read/write 事件
   3. **runAllTasks**：再去以此循环处理任务队列中的其他任务
7. 在以上两个**processSelectedKeys**步骤中，会使用 Pipeline（管道），Pipeline 中引用了 Channel，即通过 Pipeline 可以获取到对应的 Channel，Pipeline 中维护了很多的处理器（拦截处理器、过滤处理器、自定义处理器等）。
