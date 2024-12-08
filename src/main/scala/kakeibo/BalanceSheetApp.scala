package kakeibo

import cats.effect.{Async, Resource}
import edomata.backend.{Backend, eventsourcing}
import edomata.skunk.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import kakeibo.JournalEntry.AccountTitle
import skunk.Session

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BalanceSheetApp {
  import Codecs.BsEventCodec.*
  import Codecs.BalanceSheetCodec.*
  given BackendCodec[BsEvent] = CirceCodec.json
  given BackendCodec[BsNotification] = CirceCodec.json
  given BackendCodec[BalanceSheet] = CirceCodec.json

  def backend[F[_]: Async](pool: Resource[F, Session[F]]) = Backend
    .builder(BalanceSheetService)
    .use(SkunkDriver("edomata_kakeibo", pool))
//    .persistedSnapshot(maxInMem = 100)
    .build

  def apply[F[_]: Async](pool: Resource[F, Session[F]]) =
    backend(pool).map(s =>
      new BalanceSheetApp(s, s.compile(BalanceSheetService[F]))
    )
}

final case class BalanceSheetApp[F[_]](
    storage: eventsourcing.Backend[
      F,
      BalanceSheet,
      BsEvent,
      BsRejection,
      BsNotification
    ],
    service: BalanceSheetService.Handler[F]
)
