package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {
  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

//  val simpleGraph = Source(21 to 30).to(Sink.foreach(println))
//  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  //val sumFuture = source.runWith(sink)

  import scala.concurrent.ExecutionContext.Implicits.global
//  sumFuture.onComplete {
//    case Success(value) => println(s"The sum of all elements is : $value")
//    case Failure(ex) => println(s"The sum of the elements could not by computed: $ex")
//  }

  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(_ * 2 - 1)
  val simpleSink = Sink.foreach[Int](println)
//  val simpleGraph = simpleSource.via(simpleFlow).to(simpleSink)
//  val graph = simpleSource.viaMat(simpleFlow)(Keep.left).toMat(simpleSink)(Keep.right)
//  graph.run().onComplete {
//    case Success(_) => println("Stream Processing finished")
//    case Failure(ex) => println(s"Stream processing failed with :$ex")
//  }
//
//  Source(1 to 10).runWith(Sink.reduce(_ + _))
//  Source(1 to 10).runReduce(_ + _)
//
//  Sink.foreach[Int](println).runWith(Source.single(42))
//  Flow[Int].map(x => 2 * x).runWith(simpleSource, simpleSink)

  /**
   * return the last element out of a source(use Sink.last)
   * compute the total word count out of a stream of sentences
   * map, fold, reduce
   */
  val last = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
}
