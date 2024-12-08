package kakeibo

import cats.effect.{IO, IOApp}
import edomata.core.CommandMessage
import kakeibo.JournalEntry.AccountTitle.*

import java.time.{Instant, LocalDate}

object Main extends IOApp.Simple {

  private val now = IO.realTime.map(d => Instant.EPOCH.plusNanos(d.toNanos))

  override def run: IO[Unit] = Application[IO]().use(app =>
    for {
      t0 <- now
      _ <- app.balanceSheet.service(
        CommandMessage(
          "cmd-id-1",
          t0,
          "bs-1",
          BsCommand.Initialize(
            Map(Cash -> BigDecimal(1_000_000)),
            Map(AccruedLiability -> BigDecimal(100_000))
          )
        )
      )
      t1 <- now
      _ <- app.balanceSheet.service(
        CommandMessage(
          "cmd-id-2",
          t1,
          "bs-1",
          BsCommand.Journalize(
            JournalEntry(
              date = LocalDate.now(),
              debit = BankAccount,
              credit = Cash,
              amount = BigDecimal(500_000),
              description = "入金"
            )
          )
        )
      )
      t2 <- now
      _ <- app.balanceSheet.service(
        CommandMessage(
          "cmd-id-3",
          t2,
          "bs-1",
          BsCommand.Journalize(
            JournalEntry(
              date = LocalDate.now(),
              debit = AccruedLiability,
              credit = BankAccount,
              amount = BigDecimal(100_000),
              description = "foo カード"
            )
          )
        )
      )
      t3 <- now
      _ <- app.balanceSheet.service(
        CommandMessage(
          "cmd-id-4",
          t3,
          "bs-1",
          BsCommand.Journalize(
            JournalEntry(
              date = LocalDate.now(),
              debit = FoodCost,
              credit = AccruedLiability,
              amount = BigDecimal(3_000),
              description = "スーパー"
            )
          )
        )
      )
      _ <- IO.println("--- journal")
      _ <- app.balanceSheet.storage.journal.readAll.printlns.compile.drain
      _ <- IO.println("--- bs-1 aggregates")
      _ <- app.balanceSheet.storage.repository
        .history("bs-1")
        .printlns
        .compile
        .drain
      _ <- IO.println("--- outbox")
      _ <- app.balanceSheet.storage.outbox.read.printlns.compile.drain
    } yield ()
  )
}
