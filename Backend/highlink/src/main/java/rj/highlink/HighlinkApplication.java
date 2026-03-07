package rj.highlink;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("rj.highlink.mapper")
public class HighlinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighlinkApplication.class, args);
    }

}
