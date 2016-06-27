package io.grhodes.mcm.server.gcm

import java.util.concurrent.{Delayed, TimeUnit}

abstract class ScheduledJob(delay: Long) extends Delayed {
  if (delay <= 0) {
    throw new IllegalArgumentException("invalid delay: " + delay)
  }

  val scheduledTime = System.currentTimeMillis + delay

  override def getDelay(unit: TimeUnit): Long = unit.convert(scheduledTime - System.currentTimeMillis, TimeUnit.MILLISECONDS)

  override def compareTo(o: Delayed): Int = {
    if (this == o) {
      0
    } else {
      if (o == null) {
        throw new IllegalArgumentException("null delayed element")
      }

      val delay = scheduledTime
      val other = o.asInstanceOf[ScheduledJob].scheduledTime
      if (delay == other) {
        0
      } else if (delay > other) {
        1
      } else {
        -1
      }
    }


  }

  def execute(messageRelayManager: MessageRelayManager): Boolean
}
