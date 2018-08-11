package blended.itestsupport.condition

import akka.actor.Props
import akka.testkit.{TestProbe, ImplicitSender, TestActorRef}
import blended.itestsupport.condition.ConditionProvider._
import blended.itestsupport.protocol._
import blended.testsupport.TestActorSys
import org.scalatest.{Matchers, WordSpec}

class ConditionActorSpec extends WordSpec
  with Matchers {

  "The Condition Actor" should {

    "respond with a satisfied message once the condition was satisfied" in TestActorSys { testkit =>
      implicit val system = testkit.system
      val probe = TestProbe()

      val c = alwaysTrue
      val checker = TestActorRef(Props(ConditionActor(cond = c)))
      checker.tell(CheckCondition, probe.ref)
      probe.expectMsg(ConditionCheckResult(List(c), List.empty[Condition]))
    }

    "respond with a timeout message if the condition wasn't satisfied in a given timeframe" in TestActorSys { testkit =>
      implicit val system = testkit.system
      val probe = TestProbe()

      val c = neverTrue
      val checker = TestActorRef(Props(ConditionActor(cond = c)))
      checker.tell(CheckCondition, probe.ref)
      probe.expectMsg(ConditionCheckResult(List.empty[Condition],List(c)))
    }

    "respond with a satisfied message if a nested parallel condition is satisfied" in TestActorSys { testkit =>
      implicit val system = testkit.system
      val probe = TestProbe()

      val pc = ParallelComposedCondition(alwaysTrue, alwaysTrue)
      val checker = TestActorRef(Props(ConditionActor(pc)))

      checker.tell(CheckCondition, probe.ref)
      probe.expectMsg(ConditionCheckResult(pc.conditions.toList, List.empty[Condition]))
    }
  }
}
