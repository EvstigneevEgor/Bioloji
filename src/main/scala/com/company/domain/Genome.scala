package com.company.domain

/**
 * Геном клетки как иммутабельное значение и единый источник истины декодинга.
 * Строка из символов [[Genome.Alphabet]] работает как программа крошечной
 * виртуальной машины: цифры — направления, буквы — команды (`s`/`e`/`a`/`f`).
 *
 * Раньше декодинг был размазан и продублирован между шагом симуляции и
 * «расшифровкой для UI»; теперь и то и другое опирается на [[commandAt]] и
 * [[jumpTarget]].
 */
final case class Genome(code: String):
  def length: Int = code.length
  def isEmpty: Boolean = code.isEmpty
  def nonEmpty: Boolean = code.nonEmpty
  def charAt(i: Int): Char = code.charAt(i)

  /** Индекс гена под указателем `pointer` (с учётом зацикливания). */
  def index(pointer: Int): Int = pointer % length

  /**
   * Цифра-направление, стоящая сразу после команды s/e/a в позиции `g`.
   * `None`, если следующего символа нет или он не цифра 1..8.
   */
  private def dirArg(g: Int): Option[Int] =
    if g + 1 < length then
      val c = charAt(g + 1)
      Option.when(Direction.isArg(c))(Direction.of(c))
    else None

  /** Разобрать команду под указателем `pointer`. */
  def commandAt(pointer: Int): Command =
    if isEmpty then Command.Idle
    else
      val g = index(pointer)
      charAt(g) match
        case 'f'   => Command.Photosynthesis
        case 's'   => dirArg(g).map(Command.Step.apply).getOrElse(Command.Jump('s'))
        case 'e'   => dirArg(g).map(Command.Divide.apply).getOrElse(Command.Jump('e'))
        case 'a'   => dirArg(g).map(Command.Attack.apply).getOrElse(Command.Jump('a'))
        case 'c'   => dirArg(g).map(Command.Scavenge.apply).getOrElse(Command.Jump('c'))
        case other => Command.Jump(other)

  /**
   * Куда уйдёт указатель при «прыжке» (bPerehod) по символу `sym`: к первому
   * вхождению `sym` в геном; если попали туда же, где стоим — сдвиг ещё на 3.
   */
  def jumpTarget(pointer: Int, sym: Char): Int =
    val z = code.indexOf(sym.toInt)
    if z < 0 then pointer % length
    else
      val next = (pointer + z) % length
      if next != pointer then next else (pointer + z + 3) % length

  /**
   * Считаются «роднёй», если различие геномов меньше порога (в %).
   * Защищает «своих» от поедания.
   */
  def isKin(other: Genome, thresholdPct: Int): Boolean =
    val minLen = math.min(length, other.length)
    if minLen == 0 then false
    else
      val diffInPrefix = (0 until minLen).count(i => charAt(i) != other.charAt(i))
      val razn = math.abs(length - other.length) + diffInPrefix
      (razn * 100) / minLen < thresholdPct

  /**
   * Мутация при размножении: с шансом вставки нового гена добавляет символ
   * (если не достигнут предел длины), иначе заменяет случайный символ.
   * Чистая функция от `rng`: возвращает новый геном, порядок розыгрышей ГСЧ
   * сохранён 1-в-1 с прежним `Kletka.genMut`.
   */
  def mutate(rng: Rng, cfg: SimulationConfig): Genome =
    import Genome.Alphabet
    if rng.nextInt(cfg.insertChance) == 1 && length < cfg.maxGenLength then
      Genome(code + Alphabet.charAt(rng.nextInt(Alphabet.length)))
    else
      val b = rng.nextInt(length)
      val c = Alphabet.charAt(rng.nextInt(Alphabet.length))
      Genome(
        if b + 1 < length then code.substring(0, b) + c + code.substring(b + 1)
        else code.substring(0, b) + c
      )

object Genome:
  /** Алфавит генома: цифры задают направления, буквы — команды (s/e/a/f/c). */
  val Alphabet = "12345678seafc"

  /** Пустой геном (нет программы — трупы и пустые ячейки). */
  val Empty: Genome = Genome("")

  /**
   * Оставить в строке только символы [[Alphabet]] (прочие отбрасываются).
   * Используется при ручном вводе/правке генома в UI, чтобы в геном не попали
   * символы, которые ВМ не умеет декодировать.
   */
  def sanitize(s: String): String = s.filter(Alphabet.contains)
