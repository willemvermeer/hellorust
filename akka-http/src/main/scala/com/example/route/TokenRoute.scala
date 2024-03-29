package com.example.route

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import com.example.auth.TokenCreator
import com.example.db.{ DbQuery, HikariConnectionPool }
import com.example.ops.FutureTryOps.FutureTryOps
import de.heikoseeberger.akkahttpjson4s.Json4sSupport._
import org.json4s.native.Serialization
import org.json4s.{ native, DefaultFormats }

import java.util.UUID
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

case class TokenRequest(username: String, password: String)
case class TokenResponse(access_token: String, id_token: String, scope: String, expires_in: Int, token_type: String)

case class TokenRoute(dbPool: HikariConnectionPool, tokenCreator: TokenCreator)(implicit val system: ActorSystem) {
  implicit val serialization: Serialization.type = native.Serialization
  implicit val formats: DefaultFormats.type      = DefaultFormats

  private implicit def ec: ExecutionContext = system.dispatcher

  def route = path("token") {
    post {
      entity(as[TokenRequest]) { tokenRequest =>
        println(tokenRequest)
        onComplete(for {
          userInfo  <- DbQuery.userInfo(tokenRequest.username, dbPool).toFuture
          tokenPair <- tokenCreator.createTokenPair(userInfo, UUID.randomUUID().toString).toFuture
          response <- Future {
                       TokenResponse(
                         access_token = tokenPair.accessToken.rawToken,
                         id_token = tokenPair.idToken.rawToken,
                         scope = "openid profile email address phone",
                         expires_in = 100,
                         token_type = "Bearer"
                       )
                     }
        } yield response) {
          case Success(value) =>
            complete(StatusCodes.OK, value)
          case Failure(failure) =>
            failure.printStackTrace()
            complete(StatusCodes.InternalServerError, failure.getMessage)
        }
      }
    } ~ get {
      complete(StatusCodes.OK, "Try POSTing")
    }
  }

}
