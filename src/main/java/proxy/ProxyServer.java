package proxy;

import cache.CacheManager;
import filter.AccessController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 代理服务器核心：
 * 1. 用 ServerSocket 监听客户端连接
 * 2. 每来一个客户端，从线程池里取一个线程去处理（不堵死主线程）
 */
public class ProxyServer {

    private final int port;
    private final ExecutorService threadPool;
    private final CacheManager cacheManager;
    private final AccessController accessController;

    public ProxyServer(int port) {
        this.port = port;
        // 固定50个线程的线程池，可以同时处理50个客户端请求
        // 实际开发中可以换成 Executors.newCachedThreadPool() 动态扩容
        this.threadPool = Executors.newFixedThreadPool(50);
        this.cacheManager = new CacheManager();
        this.accessController = new AccessController();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[服务器] 开始监听，等待连接...");

            // 主循环：不断接受新连接
            while (true) {
                // accept() 会一直阻塞，直到有客户端连进来
                Socket clientSocket = serverSocket.accept();

                // 把这个连接扔给线程池处理，主线程立刻回来等下一个连接
                threadPool.submit(new ProxyHandler(clientSocket, cacheManager, accessController));
            }
        } catch (IOException e) {
            System.err.println("[服务器] 启动失败：" + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}
