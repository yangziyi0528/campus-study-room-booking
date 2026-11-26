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

fun main(args: Array<String>) {
    runApplication<StudyRoomApplication>(*args)
}
