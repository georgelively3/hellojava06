package com.lithespeed.hellojava06.karate.controlm;

import com.intuit.karate.junit5.Karate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ControlMKarateTest {

    @Karate.Test
    Karate testControlMApi() {
        return Karate.run("control-m-api").relativeTo(getClass());
    }
}
