package org.tribbloid.spookystuff.example.price.cloud

import org.tribbloid.spookystuff.SpookyContext
import org.tribbloid.spookystuff.actions.Wget
import org.tribbloid.spookystuff.example.ExampleCore

/**
 * Created by peng on 25/08/14.
 */
object SingleHop extends ExampleCore {

  override def doMain(spooky: SpookyContext) = {
    import spooky._
    noInput
      .fetch(
        Wget("http://www.singlehop.com/server-hosting/dedicated-servers/")
      )
      .sliceJoin("tbody tr")()
      .extract(
        "CPU" -> {_.text1("span")},
        "Core/Freq" -> {_.text("td")(2)},
        "Memory" -> {_.text("td")(3)},
        "Drive" -> {_.text("td")(4)},
        "Bandwidth" -> {_.text("td")(5)},
        "Availability" -> {_.text("td")(6)},
        "price" -> {_.text1("b")}
      ).asSchemaRDD()
  }
}