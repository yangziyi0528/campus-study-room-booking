package edu.nuistech.studyroom.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.time.Clock
import java.time.ZoneId

@SpringBootApplication
class StudyRoomApplication {
    @Bean
    fun campusClock(): Clock = Clock.system(ZoneId.of("Asia/Shanghai"))
}

@Suppress("SpreadOperator") // Forward the process arguments once to Spring Boot's vararg entry point.
fun main(args: Array<String>) {
    runApplication<StudyRoomApplication>(*args)
}
