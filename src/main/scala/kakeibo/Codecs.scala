package kakeibo

import io.circe.generic.auto.*
import io.circe.syntax.*
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import kakeibo.JournalEntry.AccountTitle

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Codecs {
  object AccountTitleCodec {
    implicit val encoder: Encoder[AccountTitle] =
      Encoder.encodeString.contramap(_.toString)
    implicit val decoder: Decoder[AccountTitle] =
      Decoder.decodeString.emap {
        case "Cash"             => Right(AccountTitle.Cash)
        case "BankAccount"      => Right(AccountTitle.BankAccount)
        case "FooPay"           => Right(AccountTitle.FooPay)
        case "FundsEquity"      => Right(AccountTitle.FundsEquity)
        case "FundsDeposit"     => Right(AccountTitle.FundsDeposit)
        case "AccruedLiability" => Right(AccountTitle.AccruedLiability)
        case "FoodCost"         => Right(AccountTitle.FoodCost)
        case "EatOut"           => Right(AccountTitle.EatOut)
        case "Salary"           => Right(AccountTitle.Salary)
        case "Dividend"         => Right(AccountTitle.Dividend)
        case other              => Left(s"Unknown AccountTitle: $other")
      }
  }

  object JournalEntryCodec {
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    implicit val encoder: Encoder[JournalEntry] = Encoder.forProduct5(
      "date",
      "debit",
      "credit",
      "amount",
      "description"
    )(je =>
      (
        je.date.format(formatter),
        je.debit,
        je.credit,
        je.amount,
        je.description
      )
    )

    implicit val decoder: Decoder[JournalEntry] = Decoder.forProduct5(
      "date",
      "debit",
      "credit",
      "amount",
      "description"
    )(
      (
          date: String,
          debit: AccountTitle,
          credit: AccountTitle,
          amount: BigDecimal,
          description: String
      ) =>
        JournalEntry(
          LocalDate.parse(date, formatter),
          debit,
          credit,
          amount,
          description
        )
    )
  }

  object BsEventCodec {
    implicit val encoder: Encoder[BsEvent] = Encoder.instance {
      case BsEvent.Initialized(assets, liabilities) =>
        Json.obj(
          "type" -> Json.fromString("Initialized"),
          "assets" -> Json.obj(assets.map { case (k, v) =>
            k.toString -> v.asJson
          }.toSeq*),
          "liabilities" -> Json.obj(liabilities.map { case (k, v) =>
            k.toString -> v.asJson
          }.toSeq*)
        )
      case BsEvent.Journalized(journalEntry) =>
        Json.obj(
          "type" -> Json.fromString("Journalized"),
          "journalEntry" -> journalEntry.asJson
        )
    }
    implicit val decoder: Decoder[BsEvent] = Decoder.instance { cursor =>
      cursor.downField("type").as[String].flatMap {
        case "Initialized" =>
          for {
            assets <- cursor
              .downField("assets")
              .as[Map[String, BigDecimal]]
              .map(_.map { case (k, v) =>
                AccountTitleCodec.decoder
                  .decodeJson(Json.fromString(k))
                  .getOrElse(
                    throw new Exception(s"Invalid AccountTitle key: $k")
                  ) -> v
              })
            liabilities <- cursor
              .downField("liabilities")
              .as[Map[String, BigDecimal]]
              .map(_.map { case (k, v) =>
                AccountTitleCodec.decoder
                  .decodeJson(Json.fromString(k))
                  .getOrElse(
                    throw new Exception(s"Invalid AccountTitle key: $k")
                  ) -> v
              })
          } yield BsEvent.Initialized(assets, liabilities)

        case "Journalized" =>
          cursor
            .downField("journalEntry")
            .as[JournalEntry]
            .map(BsEvent.Journalized.apply)

        case other =>
          Left(
            DecodingFailure(s"Unknown BsEvent type: $other", cursor.history)
          )
      }
    }
  }

  object BalanceSheetCodec {
    implicit val encoder: Encoder[BalanceSheet] = Encoder.instance {
      case BalanceSheet.NotReady =>
        Json.obj("type" -> Json.fromString("NotReady"))
      case BalanceSheet.Ready(assets, liabilities) =>
        Json.obj(
          "type" -> Json.fromString("Ready"),
          "assets" -> Json.obj(assets.map { case (k, v) =>
            k.toString -> v.asJson
          }.toSeq*),
          "liabilities" -> Json.obj(liabilities.map { case (k, v) =>
            k.toString -> v.asJson
          }.toSeq*)
        )
    }

    implicit val decoder: Decoder[BalanceSheet] = Decoder.instance { cursor =>
      cursor.downField("type").as[String].flatMap {
        case "NotReady" => Right(BalanceSheet.NotReady)
        case "Ready" =>
          for {
            assets <- cursor
              .downField("assets")
              .as[Map[String, BigDecimal]]
              .map(_.map { case (k, v) =>
                AccountTitleCodec.decoder
                  .decodeJson(Json.fromString(k))
                  .getOrElse(
                    throw new Exception(s"Invalid AccountTitle key: $k")
                  ) -> v
              })
            liabilities <- cursor
              .downField("liabilities")
              .as[Map[String, BigDecimal]]
              .map(_.map { case (k, v) =>
                AccountTitleCodec.decoder
                  .decodeJson(Json.fromString(k))
                  .getOrElse(
                    throw new Exception(s"Invalid AccountTitle key: $k")
                  ) -> v
              })
          } yield BalanceSheet.Ready(assets, liabilities)
        case other =>
          Left(
            DecodingFailure(
              s"Unknown BalanceSheet type: $other",
              cursor.history
            )
          )
      }
    }
  }
}
