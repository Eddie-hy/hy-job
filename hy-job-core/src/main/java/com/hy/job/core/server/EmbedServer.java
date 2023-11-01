package com.hy.job.core.server;

import com.hy.job.core.biz.ExecutorBiz;
import com.hy.job.core.biz.impl.ExecutorBizImpl;
import com.hy.job.core.biz.model.*;
import com.hy.job.core.thread.ExecutorRegistryThread;
import com.hy.job.core.util.GsonTool;
import com.hy.job.core.util.ThrowableUtil;
import com.hy.job.core.util.XxlJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @Author: HY
 * @Date: 2023-10-16-22:46
 * @Description:内嵌netty服务器，用于生成任务器接收发送来的请求
 */
public class EmbedServer {

    private static final Logger logger = LoggerFactory.getLogger(EmbedServer.class);

    private ExecutorBiz executorBiz;

    private Thread thread;


    public void start(final String address, final int port, final String appname, final String accessToken) {
        executorBiz = new ExecutorBizImpl();

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Netty 中的事件循环组，用于处理网络事件
                //用于接受客户端连接
                EventLoopGroup bossGroup = new NioEventLoopGroup();
                //用于处理连接后的数据交换
                EventLoopGroup workerGroup = new NioEventLoopGroup();
                ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                        0,
                        200,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(2000),
                        new ThreadFactory() {   //线程工厂，用于创建新线程
                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "xxl-job, EmbedServer bizThreadPool-" + r.hashCode());
                            }
                        },
                        new RejectedExecutionHandler() {
                            @Override
                            //拒绝策略
                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                throw new RuntimeException("xxl-job, EmbedServer bizThreadPool is EXHAUSTED!");
                            }
                        });
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                public void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
                                            .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                            .addLast(new HttpServerCodec())
                                            .addLast(new HttpObjectAggregator(5 * 1024 * 1024))  // merge request & reponse to FULL
                                            .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
                                }
                            })
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // 绑定端口
                    ChannelFuture future = bootstrap.bind(port).sync();

                    logger.info(">>>>>>>>>>> xxl-job remoting server start success, nettype = {}, port = {}", EmbedServer.class, port);

                    // 启动注册中心
                    startRegistry(appname, address);

                    // 等待服务端关闭
                    future.channel().closeFuture().sync();
                }catch (InterruptedException e) {
                    logger.info(">>>>>>>>>>> xxl-job remoting server stop.");
                } catch (Exception e) {
                    logger.error(">>>>>>>>>>> xxl-job remoting server error.", e);
                } finally {
                    // 停止服务
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() throws  Exception{
        //销毁服务线程
        if(thread != null && thread.isAlive()){
            thread.interrupt();
        }

        //停止注册器
        stopRegistry();
        logger.info(">>>>>>>>>>> xxl-job remoting server destroy success.");
    }


    // ---------------------- registry ----------------------

    /**
     * 创建netty-http服务
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest>{

        public static final Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        private ExecutorBiz executorBiz;

        private  String accessToken;

        private ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            //转换请求参数
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);

            //激活
            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                   //激活函数
                    Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);

                    //转换为json
                    String responseJson = GsonTool.toJson(responseObj);

                    //把回复写回去
                    writeResponse(ctx, keepAlive, responseJson);
                }
            });
        }

        /**
         * 执行调度器发送过来的请求
         * @param httpMethod
         * @param uri
         * @param requestData
         * @param accessTokenReq
         * @return
         */
        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq){

            //判断参数是否有效
            if (HttpMethod.POST != httpMethod) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
            }
            if (uri == null || uri.trim().length() == 0) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
            }
            if (accessToken != null
                    && accessToken.trim().length() > 0
                    && !accessToken.equals(accessTokenReq)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
            }

            //服务匹配
            try {
                switch (uri) {
                    case "/beat":
                        return executorBiz.beat();
                    case "/idleBeat":
                        IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                        return executorBiz.idleBeat(idleBeatParam);
                    case "/run":
                        TriggerParam triggerParam = GsonTool.fromJson(requestData, TriggerParam.class);
                        return executorBiz.run(triggerParam);
                    case "/kill":
                        KillParam killParam = GsonTool.fromJson(requestData, KillParam.class);
                        return executorBiz.kill(killParam);
                    case "/log":
                        LogParam logParam = GsonTool.fromJson(requestData, LogParam.class);
                        return executorBiz.log(logParam);
                    default:
                        return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return new ReturnT<String>(ReturnT.FAIL_CODE, "request error:" + ThrowableUtil.toString(e));
            }
        }

        /**
         * 响应回传
         * @param ctx
         * @param keepAlive
         * @param responseJson
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            //响应回传
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));   //  Unpooled.wrappedBuffer(responseJson)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(response);
        }

        /**
         * 确保所有挂起的写操作被刷新到远程对等方
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        /**
         * 记录了异常信息并关闭了连接
         * @param ctx
         * @param cause
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error(">>>>>>>>>>> xxl-job provider netty_http server caught exception", cause);
            ctx.close();
        }

        /**
         * 它检查事件是否是 IdleStateEvent 类型的事件。如果是，表示连接处于空闲状态，此时会关闭连接（ctx.channel().close()）以释放资源。如果不是空闲事件，它将委托给父类来处理
         * @param ctx
         * @param evt
         * @throws Exception
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();      // beat 3N, close if idle
                logger.debug(">>>>>>>>>>> xxl-job provider netty_http server close an idle channel.");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }

    }

    // ---------------------- registry ----------------------
    public void startRegistry(final String appname ,final String address){
        //启动注册器
        ExecutorRegistryThread.getInstance().start(appname, address);
    }

    public void stopRegistry(){
        //停止注册器
        ExecutorRegistryThread.getInstance().toStop();
    }

}
