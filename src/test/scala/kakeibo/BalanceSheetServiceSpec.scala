package kakeibo

import cats.Id
import cats.data.{Chain, NonEmptyChain}
import edomata.core.EdomatonResult.Accepted
import edomata.core.{CommandMessage, EdomatonResult, RequestContext}
import edomata.syntax.all.*
import kakeibo.JournalEntry.AccountTitle.{AccruedLiability, Cash}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{LocalDate, ZonedDateTime}

class BalanceSheetServiceSpec extends AnyWordSpec with Matchers {
  "BalanceSheetService" when {
    "初期化" should {
      "コマンドを受け付ける" in {
        // Given
        val initA = Map(Cash -> BigDecimal(1_000_000))
        val initL = Map(AccruedLiability -> BigDecimal(100_000))
        val initializeScenario = RequestContext(
          command = CommandMessage(
            id = "some-id",
            time = ZonedDateTime.now().toInstant,
            address = "an-address-id",
            payload = BsCommand.Initialize(initA, initL)
          ),
          state = BalanceSheet.NotReady
        )
        // When
        val result: EdomatonResult[
          BalanceSheet,
          BsEvent,
          BsRejection,
          BsNotification
        ] = BalanceSheetService[Id].execute(initializeScenario)
        // Then
        result shouldBe Accepted(
          newState = BalanceSheet.Ready(initA, initL),
          events = NonEmptyChain(BsEvent.Initialized(initA, initL)),
          notifications = Chain(
            BsNotification.BalanceSheetPrepared(
              "an-address-id",
              BalanceSheet.Ready(initA, initL)
            )
          )
        )
      }
    }
  }
}
