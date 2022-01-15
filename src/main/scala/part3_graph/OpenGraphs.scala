package part3_graph

import akka.actor.ActorSystem
import akka.stream.{FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {
  implicit val system: ActorSystem = ActorSystem("OpenGraphs")

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )

//  sourceGraph.to(Sink.foreach(println)).run()

  /*
   Complex sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1 : $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2 : $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

//  firstSource.to(sinkGraph).run()

  /**
   * Challenge - complex flow?
   * Write your own flow that's composed of two other flow
   * - one that adds 1 to a number
   * - one that does number * 10
   */

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      incrementerShape ~> multiplierShape
      FlowShape(incrementerShape.in, multiplierShape.out)
    }
  )

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()
}
