package com.company

object Kletka:
  /** Алфавит генома: цифры задают направления, буквы — команды (s/e/a/f). */
  private val Alphabet = "12345678seaf"
  /** Урон атакующему при неудачной атаке. */
  private val Retribution = 20
  /** Насколько добыча должна превосходить по энергии, чтобы быть съеденной. */
  private val PreyAdvantage = 10
  /** Бонус энергии за поедание более слабой клетки. */
  private val WeakPreyBonus = 400
  /** Бонус энергии за поедание трупа. */
  private val CorpseBonus = 800
  /** Порог различия геномов (в %), ниже которого клетки считаются роднёй. */
  private val KinshipThresholdPct = 20
  /** Максимальная длина генома. */
  private val MaxGenLength = 100
  /** Шанс мутации «1 из N». */
  private val MutationChance = 6
  /** Внутри мутации: шанс вставки нового гена «1 из N» (иначе — замена). */
  private val InsertChance = 5

class Kletka:
  import Kletka.*

  private var state: CellState = CellState.Empty
  /** Готова ли клетка действовать в этот ход. */
  var active: Boolean = true
  /** Родилась именно в этот ход (для подсветки). */
  var justBorn: Boolean = true
  /** Умерла именно в этот ход (для подсветки). */
  private var justDied: Boolean = false

  var gen: String = ""
  var energy: Int = 1
  var timeLive: Int = 1000
  var cont: Int = 0

  /**
   * Сколько энергии клетка получила из каждого источника — для окраски
   * «чем питается»: свет (фотосинтез), живая добыча (хищник), трупы (падальщик).
   */
  var fedLight: Int = 0
  var fedPrey: Int = 0
  var fedCorpse: Int = 0

  def isLive: Boolean = state == CellState.Alive
  def isDead: Boolean = justDied
  def isCorpse: Boolean = state == CellState.Corpse
  def isBorn: Boolean = justBorn

  def creategen(s: String): Unit = gen = s

  /** Переход указателя генома при невозможности выполнить команду. */
  def bPerehod(s: Char): Unit =
    active = false
    energy -= 1
    val len = gen.length
    val z = gen.indexOf(s.toInt) // s присутствует в геноме, поэтому z >= 0
    val next = (cont + z) % len
    cont = if next != cont then next else (cont + z + 3) % len

  def hash(s: Int): Int =
    (cont + gen.indexOf(gen.charAt(s).toInt)) % gen.length

  /** Считаются «роднёй», если различие геномов меньше порога. */
  def isParents(k: Kletka): Boolean =
    val minLen = Math.min(gen.length, k.gen.length)
    if minLen == 0 then false
    else
      val diffInPrefix = (0 until minLen).count(i => gen.charAt(i) != k.gen.charAt(i))
      val razn = Math.abs(gen.length - k.gen.length) + diffInPrefix
      (razn * 100) / minLen < KinshipThresholdPct

  /** Оживить клетку с заданным геномом. */
  def reviv(startGen: String): Unit =
    state = CellState.Alive
    energy = 5
    gen = startGen
    fedLight = 1
    fedPrey = 0
    fedCorpse = 0

  def itnew(): Unit =
    active = true
    justDied = false
    justBorn = false

  /** Перемещение: клетка-источник переезжает в эту ячейку. */
  def repl(src: Kletka): Unit =
    gen = src.gen
    active = src.active
    state = CellState.Alive
    justDied = false
    cont = src.cont
    energy += src.energy
    fedLight = src.fedLight
    fedPrey = src.fedPrey
    fedCorpse = src.fedCorpse
    src.del()

  /** Размножение: потомок наследует геном родителя (с шансом мутации). */
  def burn(parent: Kletka): Unit =
    if parent.active && active then
      cont = 0
      gen = parent.gen
      if Rng.int(MutationChance) == 1 then genMut()
      else active = parent.active
      state = CellState.Alive
      justDied = false
      energy += parent.energy / 3
      justBorn = true
      parent.energy -= energy
      // Унаследовать «склонность к питанию» родителя в нормированном виде,
      // чтобы потомок сразу был нужного цвета, но значения не росли из
      // поколения в поколение. Дальше окраску определит уже своё питание.
      val pt = parent.fedLight + parent.fedPrey + parent.fedCorpse
      if pt > 0 then
        val seed = 100
        fedLight = parent.fedLight * seed / pt
        fedPrey = parent.fedPrey * seed / pt
        fedCorpse = parent.fedCorpse * seed / pt
      else
        fedLight = 1
        fedPrey = 0
        fedCorpse = 0

  private def genMut(): Unit =
    if Rng.int(InsertChance) == 1 && gen.length < MaxGenLength then
      gen += Alphabet.charAt(Rng.int(Alphabet.length))
    else
      val b = Rng.int(gen.length)
      val c = Alphabet.charAt(Rng.int(Alphabet.length))
      gen =
        if b + 1 < gen.length then gen.substring(0, b) + c + gen.substring(b + 1)
        else gen.substring(0, b) + c

  def dead(): Unit =
    cont = 0
    active = false
    state = CellState.Corpse
    justDied = true
    gen = ""
    energy = 100
    timeLive = 100

  def del(): Unit =
    cont = 0
    active = false
    state = CellState.Empty
    justDied = false
    gen = ""
    energy = 1
    timeLive = 100

  def atack(attacker: Kletka): Unit =
    if !isCorpse then
      if !isParents(attacker) then
        if attacker.energy - PreyAdvantage > energy then
          attacker.energy += WeakPreyBonus
          attacker.fedPrey += WeakPreyBonus
          repl(attacker)
        else
          attacker.energy -= Retribution
          if attacker.energy <= 0 then attacker.dead()
          else attacker.cont += 1
    else
      attacker.energy += CorpseBonus
      attacker.fedCorpse += CorpseBonus
      repl(attacker)
