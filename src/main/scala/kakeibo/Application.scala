package kakeibo

import cats.effect.Async
import cats.effect.kernel.Resource
import cats.effect.std.Console
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import skunk.Session

final case class Application[F[_]](
    balanceSheet: BalanceSheetApp[F]
)

object Application {
  def apply[F[_]: Async: Network: Console](): Resource[F, Application[F]] =
    for {
      pool <- Session.pooled[F](
        host = "localhost",
        user = "postgres",
        password = Some("passwd"),
        database = "postgres",
        max = 10
      )
      bsApp <- BalanceSheetApp(pool)
    } yield new Application(bsApp)
}
