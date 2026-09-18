package com.example.equipment.config;

import com.example.equipment.util.CryptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 数据传输加解密过滤器
 * 请求：解密请求体
 * 响应：加密响应体
 * 通过 crypto.enabled 开关控制，默认关闭
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty(name = "crypto.enabled", havingValue = "true")
public class CryptoFilter implements Filter {

    @Autowired
    private CryptoProperties cryptoProperties;

    @Autowired
    private ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURI();

        // 检查是否在排除路径中
        if (isExcludedPath(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是文件上传/下载请求
        String contentType = httpRequest.getContentType();
        if (contentType != null && (contentType.contains("multipart/form-data")
                || contentType.contains("application/octet-stream"))) {
            chain.doFilter(request, response);
            return;
        }

        // GET请求没有body，只处理响应加密
        if ("GET".equalsIgnoreCase(httpRequest.getMethod())) {
            CryptoResponseWrapper responseWrapper = new CryptoResponseWrapper(httpResponse);
            chain.doFilter(request, responseWrapper);
            encryptResponse(httpResponse, responseWrapper);
            return;
        }

        // 非GET请求：解密请求体 + 加密响应体
        // 1. 解密请求
        CryptoRequestWrapper requestWrapper = decryptRequest(httpRequest);
        // 2. 加密响应
        CryptoResponseWrapper responseWrapper = new CryptoResponseWrapper(httpResponse);
        chain.doFilter(requestWrapper, responseWrapper);
        encryptResponse(httpResponse, responseWrapper);
    }

    /**
     * 解密请求体
     */
    private CryptoRequestWrapper decryptRequest(HttpServletRequest request) throws IOException {
        String body = readBody(request);
        if (body == null || body.isBlank()) {
            return new CryptoRequestWrapper(request, body);
        }

        try {
            // 请求体格式：{"encrypted": "加密后的Base64字符串"}
            Map<?, ?> map = objectMapper.readValue(body, Map.class);
            Object encrypted = map.get("encrypted");
            if (encrypted != null) {
                String decrypted = CryptoUtil.decrypt(encrypted.toString(), cryptoProperties.getSecretKey());
                log.debug("请求体解密成功: {} -> {} bytes", request.getRequestURI(), decrypted.length());
                return new CryptoRequestWrapper(request, decrypted);
            }
        } catch (Exception e) {
            log.warn("请求体解密失败，使用原始数据: {}", e.getMessage());
        }
        return new CryptoRequestWrapper(request, body);
    }

    /**
     * 加密响应体
     */
    private void encryptResponse(HttpServletResponse response, CryptoResponseWrapper wrapper) throws IOException {
        byte[] content = wrapper.getContent();
        if (content == null || content.length == 0) {
            return;
        }

        String responseBody = new String(content, StandardCharsets.UTF_8);
        try {
            String encrypted = CryptoUtil.encrypt(responseBody, cryptoProperties.getSecretKey());
            String jsonResult = objectMapper.writeValueAsString(Map.of("encrypted", encrypted));

            response.setContentLength(jsonResult.getBytes(StandardCharsets.UTF_8).length);
            response.getOutputStream().write(jsonResult.getBytes(StandardCharsets.UTF_8));
            log.debug("响应体加密成功: {} bytes -> {} bytes", content.length, jsonResult.length());
        } catch (Exception e) {
            log.warn("响应体加密失败，返回原始数据: {}", e.getMessage());
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
        }
    }

    private boolean isExcludedPath(String uri) {
        for (String pattern : cryptoProperties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 请求包装器：替换请求体为解密后的内容
     */
    static class CryptoRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        public CryptoRequestWrapper(HttpServletRequest request, String body) {
            super(request);
            this.body = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return bais.available() == 0; }
                @Override
                public boolean isReady() { return true; }
                @Override
                public void setReadListener(ReadListener listener) {}
                @Override
                public int read() { return bais.read(); }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return body.length; }

        @Override
        public long getContentLengthLong() { return body.length; }
    }

    /**
     * 响应包装器：捕获响应内容
     */
    static class CryptoResponseWrapper extends HttpServletResponseWrapper {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final ServletOutputStream outputStream = new ServletOutputStream() {
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setWriteListener(WriteListener listener) {}
            @Override
            public void write(int b) { buffer.write(b); }
        };

        public CryptoResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public ServletOutputStream getOutputStream() { return outputStream; }

        @Override
        public PrintWriter getWriter() {
            return new PrintWriter(new OutputStreamWriter(buffer, StandardCharsets.UTF_8));
        }

        public byte[] getContent() { return buffer.toByteArray(); }
    }
}
