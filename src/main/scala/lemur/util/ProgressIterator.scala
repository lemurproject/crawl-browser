package lemur.util

import java.util.Random

class ProgressIterator[T](callBack: (Int, Long, Long) => Unit, it: Iterator[T]) extends Iterator[T] {
  var iters = 0
  var tStarted = 0L
  var tPrev = 0L

  var _sample: Double = 1.0
  var _minSecs: Int = 0
  var _every: Int = 1

  // Last time when the callback was called
  var tReported = 0L

  def every(n: Int) = {
    this._every = n
    this
  }

  def minSecs(n: Int) = {
    this._minSecs = n
    this
  }

  def sample(r: Double) = {
    this._sample = r
    this
  }

  def next: T = {

    if (tStarted == 0) {
      tStarted = System.currentTimeMillis()
      tReported = tStarted
    }
    val tIter = System.currentTimeMillis()
    val tGap = (tIter - tReported) / 1000
    val total = tIter - tStarted
    val nRand = Math.random

    if (nRand <= _sample && tGap >= _minSecs && (iters % _every) == 0) {
      callBack(iters, tGap, total)
      tReported = tIter
    }
    iters += 1
    it.next
  }

  def hasNext: Boolean = it.hasNext
}