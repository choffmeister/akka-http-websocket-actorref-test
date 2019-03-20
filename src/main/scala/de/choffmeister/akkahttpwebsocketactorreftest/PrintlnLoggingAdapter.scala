package de.choffmeister.akkahttpwebsocketactorreftest

import akka.event.LoggingAdapter

object PrintlnLoggingAdapter {
  def apply() = new LoggingAdapter {
    override def isErrorEnabled: Boolean = true
    override def isWarningEnabled: Boolean = true
    override def isInfoEnabled: Boolean = true
    override def isDebugEnabled: Boolean = true

    override protected def notifyError(message: String): Unit = println(s"[ERROR] $message")
    override protected def notifyError(cause: Throwable, message: String): Unit = println(s"[ERROR] $message")
    override protected def notifyWarning(message: String): Unit = println(s"[WARN ] $message")
    override protected def notifyInfo(message: String): Unit = println(s"[INFO ] $message")
    override protected def notifyDebug(message: String): Unit = println(s"[DEBUG] $message")
  }
}
