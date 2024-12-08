package kakeibo

import cats.Monad
import kakeibo.JournalEntry.AccountTitle

enum BsCommand {
  case Initialize(
      assets: Map[AccountTitle, BigDecimal],
      liabilities: Map[AccountTitle, BigDecimal]
  )
  case Journalize(entry: JournalEntry)
}

enum BsNotification {
  case BalanceSheetPrepared(id: String, balanceSheet: BalanceSheet)
  case BalanceSheetUpdated(id: String, balanceSheet: BalanceSheet)
}

object BalanceSheetService
    extends BalanceSheet.Service[BsCommand, BsNotification] {
  def apply[F[_]: Monad]: App[F, Unit] = App.router {
    case BsCommand.Initialize(a, l) =>
      for {
        ready <- App.state.decide(_.initialize(a, l))
        aggId <- App.aggregateId
        _ <- App.publish(BsNotification.BalanceSheetPrepared(aggId, ready))
      } yield ()
    case BsCommand.Journalize(e) =>
      for {
        ready <- App.state.decide(_.journalize(e))
        aggId <- App.aggregateId
        _ <- App.publish(BsNotification.BalanceSheetUpdated(aggId, ready))
      } yield ()
  }
}
