package com.company.domain

/**
 * Сколько энергии клетка получила из каждого источника — её «рацион».
 * Иммутабельное значение вместо трёх разрозненных `var fedLight/fedPrey/fedCorpse`.
 * Используется UI для окраски «чем питается», а в домене — для наследования
 * склонности к питанию при размножении.
 */
final case class Diet(light: Int, prey: Int, corpse: Int):
  def total: Int = light + prey + corpse

  def addLight(n: Int): Diet = copy(light = light + n)
  def addPrey(n: Int): Diet = copy(prey = prey + n)
  def addCorpse(n: Int): Diet = copy(corpse = corpse + n)

  /**
   * Нормировать к сумме `seed` (для наследования потомком в стабильном виде,
   * чтобы значения не росли из поколения в поколение). Если рациона ещё нет —
   * возвращает [[Diet.LightSeed]] («по умолчанию — фотосинтетик»).
   */
  def normalized(seed: Int): Diet =
    val t = total
    if t <= 0 then Diet.LightSeed
    else Diet(light * seed / t, prey * seed / t, corpse * seed / t)

object Diet:
  /** Пустой рацион (ещё не питалась). */
  val Empty: Diet = Diet(0, 0, 0)
  /** Стартовый рацион — чистый фотосинтетик. */
  val LightSeed: Diet = Diet(1, 0, 0)
