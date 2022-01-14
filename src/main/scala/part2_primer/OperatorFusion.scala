package part2_primer

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object OperatorFusion extends App {
  implicit val system = ActorSystem("OperatorFusion")
  implicit val materializer = ActorMaterializer()

  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(_ + 1)
  val simpleFlow2 = Flow[Int].map(_ * 10)
  val simpleSink = Sink.foreach[Int](println)

  // this runs on the same actor
//  simpleSource.via(simpleFlow).via(simpleFlow2).to(simpleSink).run()

  // operator/component FUSION

  class SimpleActor extends Actor {
    override def receive: Receive = {
      case x: Int =>
        val x2 = x + 1
        val y = x2 * 10
        println(y)
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor])
  //    (1 to 1000).foreach(simpleActor ! _)
  val complexFlow = Flow[Int].map { x =>
    Thread.sleep(1000)
    x + 1
  }

  val complexFlow2 = Flow[Int].map { x =>
    Thread.sleep(1000)
    x * 10
  }

  //한개의 액터에서 동기적으로 실행됨
//  simpleSource.via(complexFlow).via(complexFlow2).to(simpleSink).run()

  // async boundary, 비동기적 실행
//  simpleSource.via(complexFlow).async // runs on one actor
//    .via(complexFlow2).async //runs on another actor
//    .to(simpleSink) //runs on a third actor
//    .run()

  //ordering guarantees, 아래 코드는 실행 순서를 보장한다
  //첫번쨰 element 가 Flow C 까지 진행된 후, 다음 element 를 진행시킨다.
//  Source(1 to 3)
//    .map({ x => println(s"Flow A : $x"); x})
//    .map({ x => println(s"Flow B : $x"); x})
//    .map({ x => println(s"Flow C : $x"); x})
//    .runWith(Sink.ignore)

  //ordering guarantees, 아래 코드는 실행 순서를 보장하지 않는다.
  //첫번쨰 element 가 Flow A 실행이 끝나면 바로 다음 element 를 Flow A 로 진행시킨다.
  Source(1 to 3)
    .map({ x => println(s"Flow A : $x"); x}).async
    .map({ x => println(s"Flow B : $x"); x}).async
    .map({ x => println(s"Flow C : $x"); x}).async
    .runWith(Sink.ignore)
}
