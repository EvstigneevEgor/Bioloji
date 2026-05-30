package com.company.domain

import scala.util.Random

/**
 * Источник случайностей симуляции как инжектируемый инстанс (а не глобальный
 * синглтон, как было раньше). Благодаря этому одновременно могут существовать
 * независимые воспроизводимые прогоны, а тесты не делят глобальное состояние.
 */
trait Rng:
  /** Случайное целое в диапазоне [0, bound). */
  def nextInt(bound: Int): Int

/** Сидируемый ГСЧ поверх [[scala.util.Random]] — детерминирован при равном seed. */
final class SeededRng(seed: Long) extends Rng:
  private val r: Random = Random(seed)
  def nextInt(bound: Int): Int = r.nextInt(bound)
