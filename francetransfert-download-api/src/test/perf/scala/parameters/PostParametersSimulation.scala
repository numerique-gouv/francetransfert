package spring.boot.api.simulation.parameters

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import spring.boot.api.simulation.PerfTestConfig.{baseUrl, durationMin, maxResponseTimeMs, meanResponseTimeMs, responseSuccessPercentage, requestPerSecond}

class PostParametersSimulation extends Simulation {

  val httpConf = http
      .baseUrl(baseUrl) // Here is the root for all relative URLs

  val scenarioTest = scenario("Post parameters scenario")
    .exec(http("Post parameters")
      .post("/parameter")
      .header("Content-Type", "application/json")
      .body(StringBody("{ \"context\": \"updated_context_test\", \"id\": \"1\", \"key\": \"updated_key_test\", \"value\": \"updated_value_test\"}"))
      .check(status.is(200))
    )
  setUp(scenarioTest.inject(
      constantUsersPerSec(requestPerSecond) during (durationMin minutes))
      .protocols(httpConf))
      .assertions(
        global.responseTime.max.lt(maxResponseTimeMs),
        global.responseTime.mean.lt(meanResponseTimeMs),
        global.successfulRequests.percent.gt(responseSuccessPercentage)
      )
}
