package part3_graph

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, Materializer}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(_ + 1) // hard computation
  val multiplier = Flow[Int].map(_ * 10) // hard computation
  val output = Sink.foreach[(Int, Int)](println)

  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ // bringssome nice operators into scope

      //step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      //step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      //step 4 - return a closed shape
      ClosedShape //Freeze the builder's shape
      // must return a shape
    } // static Graph
  ) // runnable graph

//  graph.run() // run the graph and materialize it
  /**
   * exercise 1: feed a source into 2 sinks at the same time (hint : use a broadcast)
   */
  val firstSink = Sink.foreach[Int](x => println(s"firstSink : $x"))
  val secondSink = Sink.foreach[Int](x => println(s"secondSink : $x"))

  val exerciseGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ // bringssome nice operators into scope

      val broadcast = builder.add(Broadcast[Int](2))

      input ~>  broadcast ~> firstSink
                broadcast ~> secondSink

//      broadcast.out(0) ~> firstSink
//      broadcast.out(1) ~> secondSink

      ClosedShape
    }
  )

//  exerciseGraph.run();

  /**
   * exercise 2 : The merge and the balance components
   */
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.foreach[Int](x => println(s"Sink1 from fastSource : $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Sink2 from slowSource : $x"))

  val exercise2Graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._ // bringssome nice operators into scope

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge
      slowSource ~> merge ~> balance

      balance.out(0) ~> sink1
      balance.out(1) ~> sink2

      ClosedShape
    }
  )

  exercise2Graph.run()
}
