package part4_techniques

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

object IntegreatingActors extends App {
  implicit val system = ActorSystem("IntegreatingActors")

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string :$s")
        sender() ! s"$s$s"
      case n: Int =>
        log.info(s"Just received a number: $n")
        sender() ! (2 * n)
      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numbersSource = Source(1 to 10)

  // actor as a flow
  implicit val timeOut: Timeout = Timeout(3 seconds)
  val actorBasedFlow = Flow[Int].ask(parallelism = 4)(simpleActor)

//  numbersSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numbersSource.ask[Int](parallelism = 4)(simpleActor).to((Sink.ignore)).run()

  /*
   Actor as a source
   */

  //bufferSize = 10, overflowStrategy = OverflowStrategy.dropHead
  val actorPoweredSource = Source.actorRef(
    completionMatcher = {
      case Done =>
        // complete stream immediately if we send it Done
        CompletionStrategy.immediately
    },
    // never fail the stream because of a message
    failureMatcher = PartialFunction.empty,
    bufferSize = 10,
    overflowStrategy = OverflowStrategy.dropHead
  )
  val materializedActorRef =  actorPoweredSource.to(Sink.foreach[Int](number => println(s"Actor powered flow got number : $number"))).run()
//  materializedActorRef ! 10

//  materializedActorRef ! akka.actor.Status.Success("complete")
  /*
    Actor as a destination/sink
    - an init message
    - an ack message to confirm the reception
    - a complete message
    - a function to generate a message in case the stream throws an exception
   */
  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck
      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)
      case StreamFail(ex) =>
        log.warning(s"System failed: $ex")
      case message =>
        log.info(s"Message $message has come to its final resting point.")
        sender() ! StreamAck
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithBackpressure(
    destinationActor,
    onInitMessage = StreamInit,
    onCompleteMessage = StreamComplete,
    ackMessage = StreamAck,
    onFailureMessage = throwable => StreamFail(throwable)
  )

  Source(1 to 10).to(actorPoweredSink).run()

  // Sink.actorRef() not recommended, unable to backpressure
}
