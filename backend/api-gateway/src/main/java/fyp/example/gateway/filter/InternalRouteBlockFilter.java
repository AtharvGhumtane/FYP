package fyp.example.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class InternalRouteBlockFilter implements WebFilter, Ordered {

    private static final String INTERNAL_HEADER = "X-Internal-Token";
    private static final String FORBIDDEN_JSON = "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Direct external access to internal endpoints is blocked at the Gateway\"}";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Comprehensive multi-layer check for any /internal/ route variants
        if (isInternalRoute(request)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            DataBuffer buffer = response.bufferFactory().wrap(FORBIDDEN_JSON.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }

        // 2. Strip any incoming client-supplied X-Internal-Token header and ensure X-Forwarded-For reflects actual remote address
        ServerHttpRequest.Builder reqBuilder = request.mutate()
                .headers(httpHeaders -> httpHeaders.remove(INTERNAL_HEADER));

        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            String clientIp = request.getRemoteAddress().getAddress().getHostAddress();
            reqBuilder.header("X-Forwarded-For", clientIp);
        }

        ServerHttpRequest modifiedRequest = reqBuilder.build();
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isInternalRoute(ServerHttpRequest request) {
        if (request == null) {
            return false;
        }

        URI uri = request.getURI();
        String fullUriString = (uri != null) ? uri.toString() : "";
        String rawPath = (uri != null && uri.getRawPath() != null) ? uri.getRawPath() : "";
        String authority = (uri != null && uri.getRawAuthority() != null) ? uri.getRawAuthority() : "";
        String pathValue = request.getPath().value();

        // Check if authority or host is spoofed as "internal" via //internal/...
        if (authority.equalsIgnoreCase("internal") || authority.toLowerCase().startsWith("internal:")) {
            return true;
        }

        // Check raw path, decoded paths, full URI, and request path
        return containsInternalSegment(rawPath)
                || containsInternalSegment(decodeFully(rawPath))
                || containsInternalSegment(pathValue)
                || containsInternalSegment(decodeFully(pathValue))
                || containsInternalSegment(fullUriString)
                || containsInternalSegment(decodeFully(fullUriString));
    }

    private String decodeFully(String input) {
        if (input == null) return "";
        String decoded = input;
        try {
            decoded = URLDecoder.decode(input, StandardCharsets.UTF_8);
            decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return decoded;
    }

    private boolean containsInternalSegment(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        // Normalize slashes: replace backslashes and multiple slashes with single forward slash
        String normalized = path.replace('\\', '/').replaceAll("/+", "/");

        // Strip matrix parameters (e.g. /internal;jsessionid=... -> /internal)
        normalized = normalized.replaceAll(";[^/]*", "");

        // Normalize path segments (handle /a/../internal/ -> /internal/)
        try {
            URI normalizedUri = URI.create("http://localhost" + (normalized.startsWith("/") ? "" : "/") + normalized).normalize();
            normalized = normalizedUri.getPath();
        } catch (Exception ignored) {
        }

        String lower = normalized.toLowerCase();

        return lower.equals("/internal")
                || lower.startsWith("/internal/")
                || lower.contains("/internal/")
                || lower.endsWith("/internal")
                || lower.contains("://internal/")
                || lower.contains("//internal/");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
