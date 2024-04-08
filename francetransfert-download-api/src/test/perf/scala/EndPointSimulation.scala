package spring.boot.api.simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._
import spring.boot.api.simulation.PerfTestConfig.{baseUrl, durationMin, maxResponseTimeMs, meanResponseTimeMs, responseSuccessPercentage}

class EndPointSimulation extends Simulation {
  // Define custom rampup time for this simulation
  private val rampUpTime: FiniteDuration = 10.seconds

  val httpConf = http
    .baseUrl(baseUrl) // Here is the root for all relative URLs

  val rootEndPointUsers = scenario("Root end point calls")
    .exec(http("root end point")
          .get("/swagger-ui.html")
          .check(status.is(200))
        )

  setUp(rootEndPointUsers.inject(
      constantUsersPerSec(PerfTestConfig.requestPerSecond) during (durationMin minutes))
      .protocols(httpConf))
      .assertions(
        global.responseTime.max.lt(maxResponseTimeMs),
        global.responseTime.mean.lt(meanResponseTimeMs),
        global.successfulRequests.percent.gt(responseSuccessPercentage)
      )
}