import proxy.ProxyServer;


/**
 * 启动入口
 * 运行方式：java -jar http-proxy.jar [端口号]
 * 默认端口：8888
 */
public class Main {
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("[错误] 端口号格式不对，使用默认端口 8888");
            }
        }

        System.out.println("=== HTTP 代理缓存服务器启动 ===");
        System.out.println("监听端口：" + port);
        System.out.println("浏览器代理设置：127.0.0.1:" + port);

        ProxyServer server = new ProxyServer(port);
        server.start();
    }
}
