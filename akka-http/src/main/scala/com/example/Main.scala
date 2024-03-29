package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.example.auth.TokenCreator
import com.example.db.HikariConnectionPool
import com.example.route.TokenRoute
import com.zaxxer.hikari.HikariDataSource
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object Main extends App {

  implicit val system           = ActorSystem("akka-http-simple-auth")
  implicit val executionContext = system.dispatcher

  val config = SimpleAuthConfig.load()

  // make sure we have a crypto implementation
  java.security.Security.addProvider(new BouncyCastleProvider())

  val datasource   = HikariConnectionPool(new HikariDataSource(config.db.hikari))
  val tokenCreator = TokenCreator(config.auth)
  val route        = TokenRoute(datasource, tokenCreator).route

  val keepRunning = for {
    _ <- Http()
          .newServerAt(config.http.interface, config.http.port)
          .bind(route)
          .map(_ => println("Akka http simple auth open for e-Business"))
          .recover { case ex => ex.printStackTrace() }
    waitForever <- Future.never
  } yield waitForever

  Await.ready(keepRunning, Duration.Inf)

}
