package blended.itestsupport.jms

import javax.jms._

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable}
import akka.event.LoggingReceive
import blended.util.protocol.IncrementCounter
import blended.itestsupport.jms.protocol._
import blended.jms.utils.JMSSupport

import scala.concurrent.duration._

class AkkaConsumer(
  consumerFor: ActorRef,
  connection: Connection,
  destName: String,
  subscriberName: Option[String] = None
) extends MessageListener with JMSSupport {

  var session : Option[Session] = None
  var consumer : Option[MessageConsumer] = None

  def start() : Unit = {

    session = Some(connection.createSession(false, Session.AUTO_ACKNOWLEDGE))

    session.foreach { s =>
      val dest = destination(s, destName)
      consumer = Some(subscriberName.isDefined && dest.isInstanceOf[Topic] match {
        case true => s.createDurableSubscriber(dest.asInstanceOf[Topic], subscriberName.get)
        case _ => s.createConsumer(dest)
      })
      consumer.foreach { c => c.setMessageListener(this) }
    }
  }

  def unsubscribe() : Unit = {
    consumer.foreach { c => c.close() }

    for (
      s <- session;
      subName <- subscriberName
    ) {
      s.unsubscribe(subName)
    }

    stop()
  }

  def stop() : Unit = {
    session.foreach { _.close() }
    consumerFor ! ConsumerStopped(destName)
  }

  override def onMessage(msg: Message) : Unit = {
    consumerFor ! msg
  }
}

object Consumer {
  def apply(
    connection: Connection,
    destName: String,
    subscriberName: Option[String],
    msgCounter : Option[ActorRef] = None
  ) = new Consumer(connection, destName, subscriberName, msgCounter)

  case object MsgTimeout
  case object ConsumerCreated

}

class Consumer(
  connection: Connection,
  destName: String,
  subscriberName: Option[String],
  msgCounter: Option[ActorRef]
) extends Actor with ActorLogging {

  import blended.itestsupport.jms.Consumer.{ConsumerCreated, MsgTimeout}

  implicit val eCtxt = context.dispatcher

  val idleTimeout = FiniteDuration(
    context.system.settings.config.getLong("de.wayofquality.blended.itestsupport.jms.consumerTimeout"), SECONDS
  )

  var jmsConsumer : AkkaConsumer = _
  var idleTimer : Option[Cancellable] = None

  override def preStart() : Unit = {
    super.preStart()

    jmsConsumer = new AkkaConsumer(self, connection, destName, subscriberName)
    jmsConsumer.start()

    resetTimer()
  }

  override def receive = LoggingReceive {
    case ConsumerCreated =>
      sender ! ConsumerActor(self)
    case msg : Message =>
      msg match {
        case txtMsg : TextMessage => log.debug(s"Received message ... [${msg.asInstanceOf[TextMessage].getText}]")
        case jmsMsg => log.debug(s"Received message ... [$jmsMsg]")
      }
      msgCounter.foreach { counter => counter ! IncrementCounter(1) }
      resetTimer()
    case Unsubscribe =>
      log.info(s"Unsubscribing [$subscriberName]")
      jmsConsumer.unsubscribe()
    case stopped : ConsumerStopped =>
      context.system.eventStream.publish(stopped)
      idleTimer.foreach { _.cancel() }
    case MsgTimeout =>
      log.info(s"No message received in [$idleTimeout]. Stopping subscriber.")
      jmsConsumer.stop()
    case StopConsumer => jmsConsumer.stop()
  }

  private def resetTimer() : Unit = {
    idleTimer.foreach { _.cancel() }
    idleTimer = Some(context.system.scheduler.scheduleOnce(idleTimeout, self, MsgTimeout))
  }
}
