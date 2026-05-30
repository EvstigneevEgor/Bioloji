package com.company

object Kletka:
  /** Алфавит генома: цифры задают направления, буквы — команды (s/e/a/f). */
  private val Alphabet = "12345678seaf"
  /** Урон атакующему при неудачной атаке (штраф, регулируется из UI). */
  var Retribution = 20
  /** Насколько добыча должна превосходить по энергии, чтобы быть съеденной. */
  var PreyAdvantage = 10
  /** Бонус энергии за поедание более слабой клетки (награда, регулируется из UI). */
  var WeakPreyBonus = 400
  /** Бонус энергии за поедание трупа (награда, регулируется из UI). */
  var CorpseBonus = 800
  /**
   * Через сколько тиков труп исчезает с поля сам по себе (если его не съели).
   * `-1` — трупы не исчезают никогда (поведение по умолчанию).
   */
  var CorpseDecayTicks = -1
  /** Порог различия геномов (в %), ниже которого клетки считаются роднёй. */
  var KinshipThresholdPct = 20
  /** Максимальная длина генома. */
  var MaxGenLength = 100
  /** Шанс мутации «1 из N». */
  var MutationChance = 6
  /** Внутри мутации: шанс вставки нового гена «1 из N» (иначе — замена). */
  var InsertChance = 5

  /** Коды состояния для трекера индекса (см. [[Kletka.tracker]]). */
  inline val StEmpty = 0
  inline val StAlive = 1
  inline val StCorpse = 2

  /**
   * Счётчик для выдачи уникальных идентификаторов клеткам. Атомарный, потому
   * что в многопоточном режиме `freshId()` вызывается из нескольких потоков
   * одновременно (рождение/перемещение клеток в разных полосах поля).
   */
  private val idCounter: java.util.concurrent.atomic.AtomicLong =
    new java.util.concurrent.atomic.AtomicLong(0)
  /** Выдать новый уникальный идентификатор живой клетки. */
  private def freshId(): Long = idCounter.incrementAndGet()

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
  /** Сколько тиков клетка пробыла трупом — для «тлеющего» исчезновения. */
  private var corpseAge: Int = 0

  /**
   * Уникальный идентификатор клетки (0 — нет клетки). Сохраняется при
   * перемещении клетки по полю (repl), чтобы за конкретной клеткой можно
   * было «следить» в окне генома, а не за фиксированной ячейкой поля.
   */
  var id: Long = 0

  /**
   * Сколько энергии клетка получила из каждого источника — для окраски
   * «чем питается»: свет (фотосинтез), живая добыча (хищник), трупы (падальщик).
   */
  var fedLight: Int = 0
  var fedPrey: Int = 0
  var fedCorpse: Int = 0

  /**
   * Фиксированные координаты ячейки на поле и колбэк-«трекер» индекса живых
   * клеток. Заполняются полем [[Pole]] при создании; по умолчанию — заглушка,
   * чтобы клетки в тестах работали без индекса. Код состояния:
   * [[Kletka.StAlive]] — стала живой, [[Kletka.StCorpse]] — стала трупом,
   * [[Kletka.StEmpty]] — опустела.
   */
  var px: Int = -1
  var py: Int = -1
  var tracker: (Int, Int, Int) => Unit = (_, _, _) => ()
  private inline def track(stateCode: Int): Unit = tracker(px, py, stateCode)

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
    id = freshId()
    track(StAlive)

  def itnew(): Unit =
    active = true
    justDied = false
    justBorn = false

  /**
   * Гравитация: переместить содержимое этой ячейки (живой клетки ИЛИ трупа)
   * в ячейку `dst`, сохранив состояние и все поля без изменений (энергия,
   * возраст, идентификатор и т.д.). Текущая ячейка становится пустой.
   * В отличие от [[repl]], не меняет состояние на «живое» — поэтому годится
   * и для падения трупов.
   */
  def fallInto(dst: Kletka): Unit =
    dst.state = state
    dst.active = active
    dst.justBorn = justBorn
    dst.justDied = justDied
    dst.gen = gen
    dst.energy = energy
    dst.timeLive = timeLive
    dst.cont = cont
    dst.corpseAge = corpseAge
    dst.id = id
    dst.fedLight = fedLight
    dst.fedPrey = fedPrey
    dst.fedCorpse = fedCorpse
    dst.track(if state == CellState.Alive then StAlive else StCorpse)
    del()

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
    // Это та же самая клетка, что переехала в новую ячейку — сохраняем её
    // идентификатор, чтобы окно генома продолжало следить именно за ней.
    id = src.id
    track(StAlive)
    src.del()

  /** Размножение: потомок наследует геном родителя (с шансом мутации). */
  def burn(parent: Kletka): Unit =
    if parent.active && active then
      cont = 0
      id = freshId()
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
      track(StAlive)

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
    // Труп сохраняет столько энергии, сколько было у клетки в момент смерти
    // (но не меньше нуля) — она достанется падальщику при поедании.
    energy = Math.max(energy, 0)
    corpseAge = 0
    track(StCorpse)

  /**
   * «Тление» трупа: каждый тик увеличивает возраст трупа, и когда он
   * достигает [[Kletka.CorpseDecayTicks]] — труп исчезает (становится пустой
   * ячейкой). При `CorpseDecayTicks < 0` трупы не исчезают никогда.
   * Вызывается на трупах из [[Pole.processCell]] каждый тик.
   */
  def ageCorpse(): Unit =
    if CorpseDecayTicks >= 0 then
      corpseAge += 1
      if corpseAge >= CorpseDecayTicks then del()

  def del(): Unit =
    cont = 0
    active = false
    state = CellState.Empty
    justDied = false
    gen = ""
    energy = 1
    timeLive = 100
    corpseAge = 0
    id = 0
    track(StEmpty)

  /**
   * Поглощение этой клетки/трупа атакующим. Кормовая ценность `gain` —
   * энергия цели в момент поедания, но не меньше соответствующего бонуса.
   * Она достаётся атакующему внутри [[repl]] (`energy += src.energy`),
   * поэтому задаём её здесь, а счётчик питания дополняем у атакующего
   * (repl копирует именно его значение).
   */
  private def devour(attacker: Kletka, gain: Int, prey: Boolean): Unit =
    energy = gain
    if prey then attacker.fedPrey += gain
    else attacker.fedCorpse += gain
    repl(attacker)

  def atack(attacker: Kletka): Unit =
    if !isCorpse then
      if !isParents(attacker) then
        if attacker.energy - PreyAdvantage > energy then
          // Хищник: энергия добычи, но не меньше «Бонуса за добычу».
          devour(attacker, Math.max(energy, WeakPreyBonus), prey = true)
        else
          attacker.energy -= Retribution
          if attacker.energy <= 0 then attacker.dead()
          else attacker.cont += 1
    else
      // Падальщик: энергия трупа, но не меньше «Бонуса за труп».
      devour(attacker, Math.max(energy, CorpseBonus), prey = false)
