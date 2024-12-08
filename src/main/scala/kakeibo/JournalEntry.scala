package kakeibo

import java.time.LocalDate

final case class JournalEntry(
    date: LocalDate,
    debit: JournalEntry.AccountTitle,
    credit: JournalEntry.AccountTitle,
    amount: BigDecimal,
    description: String
)

object JournalEntry {
  enum AccountTitleGroup {
    case Asset
    case Liability
    case NetAsset
    case Expense
    case Revenue
  }

  import kakeibo.JournalEntry.AccountTitleGroup.*
  enum AccountTitle(val group: AccountTitleGroup) {
    // 資産
    case Cash extends AccountTitle(Asset)
    case BankAccount extends AccountTitle(Asset)
    case FooPay extends AccountTitle(Asset)
    case FundsEquity extends AccountTitle(Asset)
    case FundsDeposit extends AccountTitle(Asset)
    // 負債
    case AccruedLiability extends AccountTitle(Liability)
    // 費用
    case FoodCost extends AccountTitle(Expense)
    case EatOut extends AccountTitle(Expense)
    // 収益
    case Salary extends AccountTitle(Revenue)
    case Dividend extends AccountTitle(Revenue)
  }
}
