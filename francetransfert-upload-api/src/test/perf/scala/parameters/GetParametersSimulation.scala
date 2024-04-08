package spring.boot.api.simulation.parameters

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import spring.boot.api.simulation.PerfTestConfig.{baseUrl, durationMin, maxResponseTimeMs, meanResponseTimeMs, responseSuccessPercentage, requestPerSecond}

class GetParametersSimulation extends Simulation {

  val httpConf = http
      .baseUrl(baseUrl) // Here is the root for all relative URLs

  val scenarioTest = scenario("Get parameters scenario")
    .exec(http("get parameters")
      .get("/parameter")
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
