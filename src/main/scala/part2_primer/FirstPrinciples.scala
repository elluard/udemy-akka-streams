package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {
  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()(system)

  //sources
  val source = Source(1 to 10)
  //sinks
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)

//  graph.run()

  // flows transform elements
//  val flow = Flow[Int].map(x => x + 1)
//  val sourceWithFlow = source.via(flow)
//  val flowWithSink = flow.to(sink)
//
//  sourceWithFlow.to(sink).run()

  // nulls are not allowed
  //use option instead of null
//  val illegalSource = Source.single[String](null)
//  illegalSource.to(Sink.foreach(println)).run()

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1)) // do not confuse an Akka stream with a "collection" Stream

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  // sinks
//  val theMostBoringSink = Sink.ignore
//  val foreachSInk = Sink.foreach[String](println)

  /**
   * Exercise: create a stream that takes the names of persons, then you will keep the first 2 names with length > 5 character
   */
  val nameList = List("Marry", "Johnson", "Thomas", "alan", "jack", "simpson")
  val nameSource = Source(nameList)
  val nameFlow = Flow[String].filter(_.length > 5)
  val takeFlow = Flow[String].take(2)
  val resultSink = Sink.foreach(println)
  nameSource.via(nameFlow).via(takeFlow).to(resultSink).run()
//  nameSource.filter(_.length > 5).take(2).runForeach(println)
}
