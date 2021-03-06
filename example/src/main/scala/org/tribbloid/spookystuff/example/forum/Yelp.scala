package org.tribbloid.spookystuff.example.forum

import org.tribbloid.spookystuff.SpookyContext
import org.tribbloid.spookystuff.actions.Wget
import org.tribbloid.spookystuff.example.ExampleCore
import org.tribbloid.spookystuff.expressions._

/**
 * Created by peng on 9/26/14.
 */
object Yelp extends ExampleCore {

  override def doMain(spooky: SpookyContext) = {
    import spooky._

    sc.parallelize(Seq(
      "http://www.yelp.com/biz/bottega-louie-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/wurstk%C3%BCche-los-angeles-2?sort_by=date_desc",
      "http://www.yelp.com/biz/daikokuya-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/pizzeria-mozza-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/sushi-gen-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/animal-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/blu-jam-caf%C3%A9-los-angeles-2?sort_by=date_desc",
      "http://www.yelp.com/biz/langers-los-angeles-2?sort_by=date_desc",
      "http://www.yelp.com/biz/roscoes-house-of-chicken-and-waffles-los-angeles-3?sort_by=date_desc",
      "http://www.yelp.com/biz/masa-of-echo-park-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/bld-los-angeles?sort_by=date_desc",
      "http://www.yelp.com/biz/providence-los-angeles-2?sort_by=date_desc"
    ))
      .fetch(
        Wget("#{_}")
      )
      .wgetExplore('* href "a.page-option.prev-next:contains(→)")(depthKey = 'page)
      .sliceJoin("div.review")(indexKey = 'row)
      .extract(
        "comment" -> (_.text1("p.review_comment")),
        "date&status" -> (_.text1("div.review-content span.rating-qualifier")),
        "stars" -> (_.attr1("div.biz-rating div div.rating-very-large meta","content")),
        "useful" -> (_.text1("div.review-wrapper > div.review-footer a.ybtn.useful span.i-wrap span.count")),
        "user_name" -> (_.text1("li.user-name a.user-display-name")),
        "user_location" -> (_.text1("li.user-location")),
        "friend_count" -> (_.text1("li.friend-count b")),
        "review_count" -> (_.text1("li.review-count b"))
      )
      .asSchemaRDD()
  }
}
