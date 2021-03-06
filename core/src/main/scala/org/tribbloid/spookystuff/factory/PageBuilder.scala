package org.tribbloid.spookystuff.factory

import java.util.Date
import java.util.concurrent.TimeUnit

import org.openqa.selenium.Dimension
import org.slf4j.LoggerFactory
import org.tribbloid.spookystuff.SpookyContext
import org.tribbloid.spookystuff.actions._
import org.tribbloid.spookystuff.entity._

import scala.collection.mutable.ArrayBuffer

//TODO: this should be minimized and delegated to resource pool
class PageBuilder(val spooky: SpookyContext){

  val startTime: Long = new Date().getTime

  val autoSave = spooky.autoSave
  val autoCache = spooky.autoCache
  val autoRestore = spooky.autoRestore

  @volatile
  private var _driver: CleanWebDriver = null

  def existingDriver: Option[CleanWebDriver] = Option(_driver)

  //mimic lazy val but retain ability to destruct it on demand
  //TODO: make sure it can only be invoked by a subroutine with a deadline, or it will take forever!
  def getDriver: CleanWebDriver = {
    if (_driver == null) {
      _driver = spooky.driverFactory.newInstance(null, spooky)

      _driver.manage().timeouts()
        .implicitlyWait(spooky.remoteResourceTimeout.toSeconds, TimeUnit.SECONDS)
        .pageLoadTimeout(spooky.remoteResourceTimeout.toSeconds, TimeUnit.SECONDS)
        .setScriptTimeout(spooky.remoteResourceTimeout.toSeconds, TimeUnit.SECONDS)

      val resolution = spooky.browserResolution
      if (resolution != null) _driver.manage().window().setSize(new Dimension(resolution._1, resolution._2))
    }
    _driver
  }

  //remember to call this! don't want thousands of phantomJS browsers opened
  def close(): Unit = {
    if (_driver != null) {
      _driver.close()
      _driver.quit()
    }
  }

  override def finalize(): Unit = {
    try {
      this.close() //this is great evil, make sure it is never called by normal means
      LoggerFactory.getLogger(this.getClass).info("PageBuilder is finalized by GC")
    }
    catch {
      case e: Throwable =>
        LoggerFactory.getLogger(this.getClass).warn("!!!!!FAIL TO CLEANED UP PAGEBUILDER!!!!!"+e)
        throw e
    }
    finally{
      super.finalize()
    }
  }

  val backtrace: ArrayBuffer[Action] = ArrayBuffer() //kept in session, but it is action deciding if they fit in or what part to export
  val realBacktrace: ArrayBuffer[Action] = ArrayBuffer() //real one excluding buffered

  private val buffer: ArrayBuffer[Action] = ArrayBuffer()
  val pages: ArrayBuffer[Page] = ArrayBuffer()

  //  TODO: Runtime.getRuntime.addShutdownHook()
  //by default drivers should be reset and reused in this case, but whatever

  //  def exe(action: Action): Array[Page] = action.exe(this)

  //lazy execution by default.
  def +=(action: Action): Unit = {
    val trace = Trace(this.backtrace :+ action)//TODO: this shouldn't happen

    this.backtrace ++= action.trunk//always put into backtrace.
    if (action.mayExport) {
      //always try to read from cache first
      val restored = if (autoRestore) {

        Page.autoRestoreLatest(trace, spooky)
      }
      else null

      if (restored != null) {
        pages ++= restored

        LoggerFactory.getLogger(this.getClass).info("cached page(s) found, won't go online")
      }
      else {
        for (buffered <- this.buffer)
        {
          this.realBacktrace ++= buffered.trunk
//          pages.clear()//TODO: need to save later version if buffered is refreshed
          buffered(this) // buffered one may have non-empty results that are restored before
        }
        buffer.clear()

        this.realBacktrace ++= action.trunk
        var batch = action(this)

        if (autoSave) batch = batch.map(_.autoSave(spooky))
        if (autoCache) Page.autoCache(batch, trace,spooky)

        pages ++= batch
      }
    }
    else {
      this.buffer += action
    }
  }

  def ++= (actions: Seq[Action]): Unit = {
    actions.foreach(this += _)
  }
}