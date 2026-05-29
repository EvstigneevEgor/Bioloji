package com.company

import scala.util.Random

/**
 * Единый источник случайностей симуляции.
 *
 * Раньше по коду были разбросаны вызовы `Math.random()`, которые невозможно
 * было ни засидировать, ни протестировать. Здесь — один генератор, который
 * можно зафиксировать через [[seed]] для воспроизводимых прогонов и тестов.
 */
object Rng:
  private var r: Random = Random()

  /** Зафиксировать seed (для воспроизводимости/тестов). */
  def seed(s: Long): Unit = r = Random(s)

  /** Случайное целое в диапазоне [0, bound). */
  def int(bound: Int): Int = r.nextInt(bound)
