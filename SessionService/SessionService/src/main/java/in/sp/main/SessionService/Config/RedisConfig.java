package in.sp.main.SessionService.Config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
public class RedisConfig {

    @Bean // It creates a connection to your Redis server
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", 6379);
    }

    @Bean
    public RedisTemplate<String , Object> redisTemplate (RedisConnectionFactory connectionFactory){
        RedisTemplate<String,Object> template = new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        //Redis stores everything as bytes —
        //so we must define how keys and values convert between Java objects ⇆ Redis bytes.

        // key serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value serializer (JSON)
        template.setValueSerializer(RedisSerializer.json());
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }

    //This method creates a RedisTemplate, the main object used to:
    //
    //read from Redis
    //
    //write to Redis
    //
    //save objects
    //
    //fetch objects
    //
    //It receives factory (connection) automatically from Spring.
}
