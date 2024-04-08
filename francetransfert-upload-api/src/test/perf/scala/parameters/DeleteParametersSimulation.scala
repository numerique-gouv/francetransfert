package spring.boot.api.simulation.parameters

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import spring.boot.api.simulation.PerfTestConfig.{baseUrl, durationMin, maxResponseTimeMs, meanResponseTimeMs, responseSuccessPercentage, requestPerSecond}

class DeleteParametersSimulation extends Simulation {

  val httpConf = http
    .baseUrl(baseUrl) // Here is the root for all relative URLs
  val scenarioTest = scenario("Delete parameters scenario")
    .exec(http("delete parameters")
      .delete("/parameter/9898898")
      .check(status.is(404))
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
