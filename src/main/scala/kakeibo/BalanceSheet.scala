package kakeibo

import cats.data.ValidatedNec
import cats.implicits.*
import edomata.core.*
import edomata.syntax.all.*
import kakeibo.BsEvent.*
import kakeibo.BsRejection.*
import kakeibo.JournalEntry.*
import kakeibo.JournalEntry.AccountTitleGroup.*

enum BsEvent {
  case Initialized(
      assets: Map[AccountTitle, BigDecimal],
      liabilities: Map[AccountTitle, BigDecimal]
  )
  case AddedJournalEntry(journalEntry: JournalEntry)
}

enum BsRejection {
  case NotReady
  case AlreadyInitialized
  case NegativeAmountJournalEntry
  case NegativeBalance
}

enum BalanceSheet {
  import kakeibo.BsEvent.*
  import kakeibo.BsRejection.*

  case NotReady
  case Ready(
      assets: Map[AccountTitle, BigDecimal],
      liabilities: Map[AccountTitle, BigDecimal]
  )

  def initialize(
      assets: Map[AccountTitle, BigDecimal],
      liabilities: Map[AccountTitle, BigDecimal]
  ): Decision[BsRejection, BsEvent, Ready] =
    this
      .decide {
        case NotReady    => Decision.accept(Initialized(assets, liabilities))
        case Ready(_, _) => Decision.reject(AlreadyInitialized)
      }
      .validate(_.mustBeReady)

  def journalize(
      entry: JournalEntry
  ): Decision[BsRejection, BsEvent, Ready] =
    this
      .perform(mustBeReady.toDecision.flatMap { _ =>
        if entry.amount > 0
        then Decision.accept(AddedJournalEntry(entry))
        else Decision.reject(NegativeAmountJournalEntry)
      })
      .validate(_.mustNotBeNegativeBalance)

  private def mustBeReady: ValidatedNec[BsRejection, Ready] = this match
    case bs @ Ready(_, _) => bs.validNec
    case NotReady         => BsRejection.NotReady.invalidNec

  private def mustNotBeNegativeBalance: ValidatedNec[BsRejection, Ready] =
    mustBeReady andThen { bs =>
      if bs.assets.forall { case (_, amount) => amount >= 0 } &&
        bs.liabilities.forall { case (_, amount) => amount >= 0 }
      then bs.validNec
      else NegativeBalance.invalidNec
    }
}

object BalanceSheet extends DomainModel[BalanceSheet, BsEvent, BsRejection] {

  override def initial: BalanceSheet = NotReady

  override def transition
      : BsEvent => BalanceSheet => ValidatedNec[BsRejection, BalanceSheet] = {
    case Initialized(as, ls) => _ => Ready(as, ls).validNec
    case AddedJournalEntry(e) =>
      _.mustBeReady.map { bs =>
        bs.copy(
          assets = calcNewAssets(bs, e),
          liabilities = calcNewLiabilities(bs, e)
        )
      }
  }

  private def calcNewAssets(
      bs: Ready,
      e: JournalEntry
  ): Map[AccountTitle, BigDecimal] = {
    val inc =
      if e.debit.group == Asset
      then
        Some(
          e.debit -> (bs.assets.getOrElse(e.debit, BigDecimal(0)) + e.amount)
        )
      else None

    val dec =
      if e.credit.group == Asset
      then
        Some(
          e.credit -> (bs.assets.getOrElse(e.credit, BigDecimal(0)) - e.amount)
        )
      else None

    bs.assets ++ inc.toMap ++ dec.toMap
  }

  private def calcNewLiabilities(
      bs: Ready,
      e: JournalEntry
  ): Map[AccountTitle, BigDecimal] = {
    val inc =
      if e.credit.group == Liability
      then
        Some(
          e.credit -> (bs.liabilities
            .getOrElse(e.credit, BigDecimal(0)) + e.amount)
        )
      else None

    val dec =
      if e.debit.group == Liability
      then
        Some(
          e.debit -> (bs.liabilities
            .getOrElse(e.debit, BigDecimal(0)) - e.amount)
        )
      else None

    bs.liabilities ++ inc.toMap ++ dec.toMap
  }
}
