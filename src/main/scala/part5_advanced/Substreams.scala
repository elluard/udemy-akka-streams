package part5_advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Success, Failure}

object Substreams extends App {
  implicit val system = ActorSystem("Substreams")

  import system.dispatcher

  // 1- grouping a stream by a certain function
  val wordsSource = Source(List("Akka", "is", "amazing", "learning", "substreams"))
  val groups = wordsSource.groupBy(30, word => if(word.isEmpty) '\u0000' else word.toLowerCase().charAt(0))

  /*
    groupBy 함수는 각 조건별로 나눈 그룹을 substream 으로 만들어서 리턴한다.
    각 substream 별로 fold 를 하면, 당연하게도, 각각의 substream 에서 fold 가 일어난다.
    아래 코드의 실행 결과
    I just received is, count is 1
    I just received Akka, count is 1
    I just received amazing, count is 2
    I just received learning, count is 1
    I just received substreams, count is 1
   */
  groups.to(Sink.fold(0)((count, word) => {
    val newCount = count + 1
    println(s"I just received $word, count is $newCount")
    newCount
  }))
//    .run()

  // 2 - merge substreams back
  val textSource = Source(List(
    "I love Akka Streams",
    "this is amazing",
    "learning from Rock the JVM"
  ))

  val totalCharCOuntFuture = textSource
    .groupBy(2, _.length % 2)
    .map(_.length) //substream 이 나뉘어짐, 비용이 큰 연산을 여기서 실행한다.
    .mergeSubstreamsWithParallelism(2) //parallelism 파라미터는 substream 의 최대값을 넘긴다. groupBy 의 maxSubStreams 값 보다 작을 경우 문제가 생길 수 있음.
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  totalCharCOuntFuture.onComplete {
    case Success(value) => println(s"Total char count: $value")
    case Failure(ex) => println(s"Char computation failed: $ex")
  }

  // 3 - splitting a stream into substreams, when a condition is met

  val text =
    "I love Akka Streams\n" +
    "this is amazing\n" +
    "learning from Rock the JVM\n"

  val anotherCharCountFuture = Source(text.toList)
    .splitWhen(c => c == '\n')
    .filter(_ != '\n')
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCharCountFuture.onComplete {
    case Success(value) => println(s"Total char count alternative: $value")
    case Failure(ex) => println(s"Char computation failed: $ex")
  }

  // 4 - flattening
  val simpleSource = Source(1 to 5)
  simpleSource.flatMapConcat(x => Source(x to (3 * x))).runWith(Sink.foreach(println))
  simpleSource.flatMapMerge(2, x => Source(x to (3 * x))).runWith(Sink.foreach(println))

}
