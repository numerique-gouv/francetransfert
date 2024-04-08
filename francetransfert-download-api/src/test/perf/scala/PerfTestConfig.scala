package spring.boot.api.simulation

import spring.boot.api.simulation.SystemPropertiesUtil._

object PerfTestConfig {
  val baseUrl = getAsStringOrElse("baseUrl", System.getProperty("baseUrl"))
  val requestPerSecond = getAsDoubleOrElse("requestPerSecond", System.getProperty("requestPerSecond").toDouble)
  val durationMin = getAsDoubleOrElse("durationMin", System.getProperty("durationMin").toDouble)
  val meanResponseTimeMs = getAsIntOrElse("meanResponseTimeMs", System.getProperty("maxResponseTimeMs").toInt)
  val maxResponseTimeMs = getAsIntOrElse("maxResponseTimeMs", System.getProperty("meanResponseTimeMs").toInt)
  val responseSuccessPercentage = System.getProperty("responseSuccessPercentage").toInt
}