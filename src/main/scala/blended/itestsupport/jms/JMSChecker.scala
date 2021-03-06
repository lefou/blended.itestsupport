package blended.itestsupport.jms

import java.util.concurrent.atomic.AtomicBoolean

import javax.jms.ConnectionFactory
import akka.actor._
import blended.itestsupport.condition.{AsyncChecker, AsyncCondition}
import blended.jms.utils.JMSSupport
import blended.util.logging.Logger

import scala.concurrent.Future
import scala.concurrent.duration._

object JMSAvailableCondition {
  def apply(cf: ConnectionFactory, t: Option[FiniteDuration] = None)(implicit system: ActorSystem) =
    AsyncCondition(Props(JMSChecker(cf)), s"JMSAvailableCondition($cf)", t)
}

private[jms] object JMSChecker {
  def apply(cf: ConnectionFactory) = new JMSChecker(cf)
}

private[jms] class JMSChecker(cf: ConnectionFactory) extends AsyncChecker with JMSSupport {

  private val log : Logger = Logger[JMSChecker]
  var connected: AtomicBoolean = new AtomicBoolean(false)
  var connecting: AtomicBoolean = new AtomicBoolean(false)

  override def supervisorStrategy = OneForOneStrategy() {
    case _ => SupervisorStrategy.Stop
  }

  override def performCheck(cond: AsyncCondition): Future[Boolean] = {

    log.debug(s"Checking JMS connection...[$cf]")

    if ((!connected.get()) && (!connecting.get())) {
      connecting.set(true)

      withConnection { _ =>
        connected.set(true)
      }(cf) foreach { t =>
        log.debug(s"Error checking JMS connection")
        log.trace(t)(t.getMessage)
      }

      connecting.set(false)
    }

    Future(connected.get())
  }
}
