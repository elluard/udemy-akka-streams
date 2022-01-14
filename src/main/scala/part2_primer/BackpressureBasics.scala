package part2_primer

import akka.actor.ActorSystem
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object BackpressureBasics extends App {
  implicit val system = ActorSystem("BackpressureBasics")

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink : $x")
  }

  //한 개의 액터에서 동기적으로 실행되므로 backpressure 가 필요없음
//  fastSource.to(slowSink).run()

  //여러개의 액터에서 비동기적으로 실행되므로 backpressure 가 내부적으로 발생한다.
//  fastSource.async.to(slowSink).run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming $x")
    x + 1
  }

  //backpressure 가 발생한다. 때문에 Incoming X 는 매우 빠른 속도로 찍히고,
  //Sink x 는 천천히 찍힌다. Sink 버퍼가 빈 후, Incoming 작업이 다시 일어나고
  //Sink 에서 작업을 다시 처리하길 기다린다.
  fastSource.async
    .via(simpleFlow).async
//    .to(slowSink).run()

  //backpressure 를 사용자 정의하는 경우, dropHead 의 경우에는 buffer 에 가장 오래 쌓인 것 부터 버린다.
  val bufferFlow = simpleFlow.buffer(10, overflowStrategy = OverflowStrategy.fail)
//  fastSource.async
//    .via(bufferFlow).async
//    .to(slowSink)
//    .run()

  /**
   * 버퍼가 가득 찰 경우 전락
   * OverflowStrategy.dropHead -> 가장 오래된 것 부터 버림
   * OverflowStrategy.dropTail -> 최근에 온 것 부터 버림
   * OverflowStrategy.dropNew -> 버퍼에 insert 를 막고 그냥 버림
   * OverflowStrategy.dropBuffer -> 버퍼 전체를 비움
   * OverflowStrategy.backpressure -> backpressure 발생
   * OverflowStrategy.fail -> 액터 실패처리, 액터를 종료함
   */

  // Throttling, Source 에서 데이터 전달 속도를 조정한다.
  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
