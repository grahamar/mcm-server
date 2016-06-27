package io.grhodes.mcm.server.gcm

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{DelayQueue, Executors}

import com.gilt.gfc.logging.Loggable

import scala.util.control.NonFatal

/**
  * Message Relay Manager to regulate the outbound message/stanza delivery
  * to the GCM provider
  */
class MessageRelayManager(val gcmMessageStore: GCMMessageStore) extends Loggable {

  private val executorService = Executors.newFixedThreadPool(50)
  private val queue = new DelayQueue[ScheduledJob]()
  private val keepRunning = new AtomicBoolean(true)
  private val schedulerFuture = executorService.submit(new JobScheduler(this))

  def shutdown(): Boolean = {
    this.keepRunning.set(false)
    gcmMessageStore.shutdown()
    queue.clear()
    schedulerFuture.cancel(true)
    true
  }

  def addJob(job: ScheduledJob): Boolean = queue.add(job)

  class JobScheduler(relayManager: MessageRelayManager) extends Runnable {
    override def run(): Unit = {
      while (keepRunning.get()) {
        try {
          val scheduledJob = queue.take()
          executorService.execute(new Runnable() {
            override def run(): Unit = scheduledJob.execute(relayManager)
          });
        } catch {
          case NonFatal(e) =>
            error("JobScheduler.run() got exception:", e)
        }
      }
    }
  }

}
