package org.tribbloid.spookystuff.example.scientific

import org.tribbloid.spookystuff.SpookyContext
import org.tribbloid.spookystuff.actions._
import org.tribbloid.spookystuff.expressions._
import org.tribbloid.spookystuff.example.ExampleCore

/**
 * Created by peng on 06/07/14.
 */
object GoogleScholar extends ExampleCore {

  override def doMain(spooky: SpookyContext) = {
    import spooky._

    sc.parallelize(Seq("Large scale distributed deep networks"))
      .fetch(
        Visit("http://scholar.google.com/")
          +> WaitFor("form[role=search]")
          +> TextInput("input[name=\"q\"]","#{_}")
          +> Submit("button#gs_hp_tsb")
          +> WaitFor("div[role=main]")
      )
      .extract(
        "title" -> (_.text1("div.gs_r h3.gs_rt a")),
        "citation" -> (_.text1("div.gs_r div.gs_ri div.gs_fl a:contains(Cited)"))
      )
      .visitJoin('* href "div.gs_r div.gs_ri div.gs_fl a:contains(Cited)", limit = 1)()
      .wgetExplore('* href "div#gs_n td[align=left] a")(depthKey = 'page)
      .sliceJoin("div.gs_r")()
      .extract(
        "citation_title" -> (_.text1("h3.gs_rt a")),
        "citation_abstract" -> (_.text1("div.gs_rs"))
      )
      .wgetJoin('* href "div.gs_md_wp a")()
      .saveContent(select = _.get("citation_title"))
      .asSchemaRDD()
  }
}