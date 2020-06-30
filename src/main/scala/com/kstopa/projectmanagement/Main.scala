package com.kstopa.projectmanagement

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._

object Main extends IOApp {
  def run(args: List[String]) =
    ProjectmanagementServer.create()
}
