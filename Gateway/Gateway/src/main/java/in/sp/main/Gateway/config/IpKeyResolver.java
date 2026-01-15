package in.sp.main.Gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
public class IpKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Mono.just(
                Objects.requireNonNull(exchange.getRequest()
                                .getRemoteAddress())
                        .getAddress()
                        .getHostAddress()
        );
    }
}
// ye code  ->
//Har request ka IP address nikalta hai →
//usko ek unique key bana deta hai →
//Gateway us key par rate limiting lagata hai.
