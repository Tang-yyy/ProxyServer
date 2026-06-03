package proxy;

import cache.CacheEntry;
import cache.CacheManager;
import filter.AccessController;
import log.ProxyLogger;


import java.io.*;
import java.net.Socket;
import java.net.URL;

/**
 * 处理单个客户端请求的工作单元（实现 Runnable，交给线程池运行）
 *
 * 处理流程：
 *   1. 读取客户端发来的 HTTP 请求
 *   2. 解析请求方法（GET / CONNECT 等）、目标 URL、请求头
 *   3. 访问控制检查（黑名单过滤）
 *   4. 查缓存 → 命中则直接返回缓存内容
 *   5. 未命中 → 转发请求给目标服务器 → 把响应返回客户端，同时写缓存
 *   6. 记录日志
 */
public class ProxyHandler implements Runnable {

    private static final int TIMEOUT_MS = 10_000;  // 连接目标服务器的超时时间

    private final Socket clientSocket;
    private final CacheManager cacheManager;
    private final AccessController accessController;

    public ProxyHandler(Socket clientSocket, CacheManager cacheManager, AccessController accessController) {
        this.clientSocket = clientSocket;
        this.cacheManager = cacheManager;
        this.accessController = accessController;
    }

    @Override
    public void run() {
        try {
            clientSocket.setSoTimeout(TIMEOUT_MS);
            handleRequest();
        } catch (IOException e) {
            // 超时或者客户端断开连接，正常情况，不用慌
            System.err.println("[Handler] 连接异常：" + e.getMessage());
        } finally {
            try { clientSocket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleRequest() throws IOException {
        InputStream clientIn   = clientSocket.getInputStream();
        OutputStream clientOut = clientSocket.getOutputStream();

        // ===== 第一步：读取请求的第一行，例如 "GET http://www.baidu.com/ HTTP/1.1" =====
        BufferedReader reader = new BufferedReader(new InputStreamReader(clientIn));
        String requestLine = reader.readLine();

        if (requestLine == null || requestLine.isEmpty()) return;

        System.out.println("[请求] " + requestLine);

        String[] parts = requestLine.split(" ");
        if (parts.length < 3) return;

        String method  = parts[0];   // GET / POST / CONNECT
        String url     = parts[1];   // http://www.baidu.com/ 或者 baidu.com:443
        String version = parts[2];   // HTTP/1.1

        // ===== 第二步：继续读取剩余的请求头 =====
        StringBuilder headers = new StringBuilder();
        headers.append(requestLine).append("\r\n");
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            headers.append(line).append("\r\n");
        }
        headers.append("\r\n");

        // ===== 第三步：访问控制 =====
        String host = extractHost(url, method);
        if (!accessController.isAllowed(host)) {
            sendBlockedResponse(clientOut, host);
            ProxyLogger.log(method, url, 403, false);
            return;
        }

        // ===== 第四步：根据请求方法分类处理 =====
        if ("CONNECT".equalsIgnoreCase(method)) {
            // HTTPS 请求：建立隧道（拓展功能，先返回200让浏览器建立SSL）
            handleConnect(clientSocket, url);
        } else {
            // HTTP 请求：查缓存 or 转发
            handleHttpRequest(method, url, headers.toString(), clientOut);
        }
    }

    /**
     * 处理普通 HTTP 请求（GET/POST 等）
     */
    private void handleHttpRequest(String method, String url, String requestHeaders, OutputStream clientOut) throws IOException {
        // === 查缓存（只对 GET 请求缓存）===
        if ("GET".equalsIgnoreCase(method)) {
            CacheEntry cached = cacheManager.get(url);
            if (cached != null && !cached.isExpired()) {
                // 缓存命中！直接把缓存内容返回给客户端
                System.out.println("[缓存] 命中：" + url);
                clientOut.write(cached.getResponseBytes());
                clientOut.flush();
                ProxyLogger.log(method, url, 200, true);
                return;
            }
        }

        // === 缓存未命中，转发给目标服务器 ===
        URL parsedUrl = new URL(url);
        String targetHost = parsedUrl.getHost();
        int targetPort    = parsedUrl.getPort() == -1 ? 80 : parsedUrl.getPort();

        try (Socket serverSocket = new Socket(targetHost, targetPort)) {
            serverSocket.setSoTimeout(10_000);

            OutputStream serverOut = serverSocket.getOutputStream();
            InputStream  serverIn  = serverSocket.getInputStream();

            // 把客户端的请求原样转发给目标服务器
            serverOut.write(requestHeaders.getBytes());
            serverOut.flush();

            // 读取目标服务器的响应，同时：
            //   - 转发给客户端
            //   - 如果是 GET 请求，把响应存入缓存
            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int bytesRead;
            while ((bytesRead = serverIn.read(buf)) != -1) {
                clientOut.write(buf, 0, bytesRead);   // 转发给客户端
                if ("GET".equalsIgnoreCase(method)) {
                    responseBuffer.write(buf, 0, bytesRead); // 同时写入缓冲区
                }
            }
            clientOut.flush();

            // 把响应存入缓存
            if ("GET".equalsIgnoreCase(method) && responseBuffer.size() > 0) {
                // 只缓存小于 2MB 的响应，太大的就不缓存了
                if (responseBuffer.size() < 2 * 1024 * 1024) {
                    cacheManager.put(url, new CacheEntry(responseBuffer.toByteArray()));
                    System.out.println("[缓存] 已存储：" + url);
                }
            }

            ProxyLogger.log(method, url, 200, false);

        } catch (IOException e) {
            // 连不上目标服务器
            sendErrorResponse(clientOut, 502, "Bad Gateway - 无法连接到目标服务器");
            ProxyLogger.log(method, url, 502, false);
        }
    }

    /**
     * 处理 HTTPS CONNECT 请求（拓展功能）
     * 思路：告诉客户端"隧道已建立"，然后把客户端和服务器的数据流互相转发
     */
    private void handleConnect(Socket clientSocket, String hostPort) {
        // TODO: 实现 HTTPS 隧道转发
        // 步骤：
        // 1. 解析 hostPort（格式是 "baidu.com:443"）
        // 2. 连接目标服务器的443端口
        // 3. 给客户端回 "HTTP/1.1 200 Connection Established\r\n\r\n"
        // 4. 开两个线程：一个把客户端数据转发给服务器，另一个反过来
        System.out.println("[HTTPS] CONNECT 隧道（待实现）：" + hostPort);
        try {
            PrintWriter pw = new PrintWriter(clientSocket.getOutputStream());
            pw.print("HTTP/1.1 200 Connection Established\r\n\r\n");
            pw.flush();
        } catch (IOException e) {
            System.err.println("[HTTPS] 隧道建立失败：" + e.getMessage());
        }
    }

    // ========== 工具方法 ==========

    /** 从 URL 或 CONNECT 请求里提取主机名 */
    private String extractHost(String url, String method) {
        if ("CONNECT".equalsIgnoreCase(method)) {
            return url.split(":")[0];
        }
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    /** 返回 403 被拦截的响应 */
    private void sendBlockedResponse(OutputStream out, String host) throws IOException {
        String body = "<html><body><h1>403 Forbidden</h1><p>访问 " + host + " 已被代理服务器拦截</p></body></html>";
        String response = "HTTP/1.1 403 Forbidden\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + body.getBytes("UTF-8").length + "\r\n"
                + "\r\n"
                + body;
        out.write(response.getBytes("UTF-8"));
        out.flush();
    }

    /** 返回通用错误响应 */
    private void sendErrorResponse(OutputStream out, int code, String msg) throws IOException {
        String body = "<html><body><h1>" + code + "</h1><p>" + msg + "</p></body></html>";
        String response = "HTTP/1.1 " + code + " Error\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + body.getBytes("UTF-8").length + "\r\n"
                + "\r\n"
                + body;
        out.write(response.getBytes("UTF-8"));
        out.flush();
    }
}
