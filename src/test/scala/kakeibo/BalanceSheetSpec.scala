package kakeibo

import cats.data.NonEmptyChain
import edomata.core.Decision.{Accepted, Rejected}
import kakeibo.BalanceSheet.Ready
import kakeibo.BsEvent.*
import kakeibo.BsRejection.*
import kakeibo.JournalEntry.AccountTitle
import kakeibo.JournalEntry.AccountTitle.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class BalanceSheetSpec extends AnyWordSpec with Matchers {

  "BalanceSheet" when {
    "初期化" should {
      "任意の資産と負債を受け取り、準備ができる" in {
        // Given
        val initialAssets = Map(
          Cash -> BigDecimal(1_000_000),
          FundsDeposit -> BigDecimal(100_000),
          FundsEquity -> BigDecimal(50_000)
        )
        val initialLiabilities = Map(
          AccruedLiability -> BigDecimal(100_000)
        )
        // When
        val result = BalanceSheet.NotReady.initialize(
          assets = initialAssets,
          liabilities = initialLiabilities
        )
        // Then
        println(result)
        result shouldBe Accepted(
          NonEmptyChain(Initialized(initialAssets, initialLiabilities)),
          Ready(initialAssets, initialLiabilities)
        )
      }
      "準備が完了している場合は初期化できない" in {
        // Given, When
        val result = BalanceSheet
          .Ready(Map.empty, Map.empty)
          .initialize(Map.empty, Map.empty)
        // Then
        result shouldBe Rejected(NonEmptyChain(AlreadyInitialized))
      }
    }
    "仕訳の記帳" should {
      "BS を更新する" in {
        // Given
        val initA = Map(Cash -> BigDecimal(1_000_000))
        val initL = Map(AccruedLiability -> BigDecimal(100_000))
        val entry1 = JournalEntry(
          date = LocalDate.now(),
          debit = BankAccount,
          credit = Cash,
          amount = BigDecimal(500_000),
          description = "入金"
        )
        val entry2 = JournalEntry(
          date = LocalDate.now(),
          debit = AccruedLiability,
          credit = BankAccount,
          amount = BigDecimal(100_000),
          description = "foo カード"
        )
        val entry3 = JournalEntry(
          date = LocalDate.now(),
          debit = FoodCost,
          credit = AccruedLiability,
          amount = BigDecimal(3_000),
          description = "スーパー"
        )
        // When
        val result = BalanceSheet.NotReady
          .initialize(initA, initL)
          .flatMap(_.journalize(entry1))
          .flatMap(_.journalize(entry2))
          .flatMap(_.journalize(entry3))
        // Then
        result shouldBe Accepted(
          NonEmptyChain(
            Initialized(initA, initL),
            AddedJournalEntry(entry1),
            AddedJournalEntry(entry2),
            AddedJournalEntry(entry3)
          ),
          Ready(
            assets = Map(
              Cash -> BigDecimal(500_000),
              BankAccount -> BigDecimal(400_000)
            ),
            liabilities = Map(
              AccruedLiability -> BigDecimal(3_000)
            )
          )
        )
      }
      "負数の金額を持つ仕訳は記帳できない" in {
        // Given
        val initA = Map(Cash -> BigDecimal(1_000_000))
        val initL = Map.empty[AccountTitle, BigDecimal]
        val entry = JournalEntry(
          date = LocalDate.now(),
          debit = BankAccount,
          credit = Cash,
          amount = BigDecimal(-500_000),
          description = "入金"
        )
        // When
        val result = BalanceSheet.NotReady
          .initialize(initA, initL)
          .flatMap(_.journalize(entry))
        // Then
        result shouldBe Rejected(NonEmptyChain(NegativeAmountJournalEntry))
      }
      "負数の残高にはできない" in {
        // Given
        val initA = Map(Cash -> BigDecimal(1_000_000))
        val initL = Map.empty[AccountTitle, BigDecimal]
        val entry = JournalEntry(
          date = LocalDate.now(),
          debit = BankAccount,
          credit = Cash,
          amount = BigDecimal(2_000_000),
          description = "入金"
        )
        // When
        val result = BalanceSheet.NotReady
          .initialize(initA, initL)
          .flatMap(_.journalize(entry))
        // Then
        result shouldBe Rejected(NonEmptyChain(NegativeBalance))
      }
    }
  }
}
