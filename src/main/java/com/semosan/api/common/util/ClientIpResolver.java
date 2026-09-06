package com.semosan.api.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 요청의 실제 클라이언트 IP 를 판별한다.
 *
 * 배포 구성은 클라이언트 → nginx-ingress → 앱 의 1홉이다.
 * nginx 는 자신이 관측한 피어 IP 를 X-Forwarded-For 의 마지막에 붙이므로(또는 헤더를 그 값으로 덮으므로),
 * 마지막 항목만이 위조 불가능한 값이다. 클라이언트가 헤더를 직접 채워 보내도 그 값들은 앞쪽에 남는다.
 *
 * 첫 항목을 쓰면 헤더만 바꿔가며 레이트리밋을 무제한 우회할 수 있으므로 절대 사용하지 않는다.
 * 프록시 홉이 늘어나면(CDN/LB 추가) 이 클래스의 판별 기준을 함께 갱신해야 한다.
 */
public final class ClientIpResolver {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwardedFor)) {
            String[] entries = forwardedFor.split(",");
            for (int i = entries.length - 1; i >= 0; i--) {
                String candidate = entries[i].trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
