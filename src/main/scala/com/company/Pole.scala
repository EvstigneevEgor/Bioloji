package com.company

object Pole:
  /** Стоимость деления и шага/атаки (регулируется из окна настроек). */
  var EnergyForDel = 150
  var EnergyForStep = 250
  /** Порог энергии для принудительного деления и его стоимость. */
  var ReproduceThreshold = 150 * 50
  var ForcedReproduceCost = 150 * 40
  /** Энергия стартовых клеток. */
  var StartEnergy = 150
  /** Стартовые геномы для случайных клеток. */
  private val StartGenes =
    Array("fffffffffffffffffffff", "ffffffffffffffff", "f", "f", "f", "f", "f", "f")
  /** Направления для принудительного деления при переизбытке энергии. */
  private val Bervz = Array(1, 3, 5, 7)
  /** Максимальное «солнце» (фотосинтез) летом/зимой: energy += (H - j) * mult / H. */
  var SummerLight = 100
  var WinterLight = 25
  /** Длительность лета и зимы в тиках симуляции. */
  var SummerTicks = 200
  var WinterTicks = 200

  /**
   * Гравитация: каждые столько тиков все клетки и трупы смещаются на 1 ячейку
   * вниз (если место под ними свободно). 0 — гравитация выключена.
   */
  var GravityTicks = 400

  /** Seed ГСЧ (применяется при сбросе симуляции) — для воспроизводимых прогонов. */
  var Seed: Long = 42

/** Тип действия гена под указателем — для наглядного окна генома. */
enum GenAction:
  case Photosynthesis, Step, Divide, Attack, Jump, Idle

/**
 * Read-only «расшифровка» текущего шага клетки: что за команда стоит под
 * указателем генома и что она сделает в этот тик. Зеркалит логику [[Pole.itr]],
 * но НИЧЕГО не меняет в состоянии — нужна только для отрисовки в GenomeWindow.
 *
 *  - `pointer`     — индекс текущего гена (`cont % len`);
 *  - `direction`   — НАСТОЯЩЕЕ направление 1..8 (9 = стоять) для s/e/a;
 *  - `targetReady` — выполнимо ли действие по соседу (s/e: есть свободная
 *                    ячейка; a: есть кого атаковать);
 *  - `feasible`    — хватает ли энергии на команду;
 *  - `energyCost`  — стоимость команды; `energyGain` — доход (фотосинтез);
 *  - `nextPointer` — куда уйдёт указатель (для боя — приблизительно);
 *  - `jumpTo`      — цель «прыжка» bPerehod;
 *  - `preNote`     — предупреждение до выполнения команды (смерть/перенаселение).
 */
case class StepExplanation(
    pointer: Int,
    symbol: Char,
    action: GenAction,
    direction: Option[Int],
    targetReady: Option[Boolean],
    feasible: Boolean,
    energyCost: Int,
    energyGain: Int,
    nextPointer: Int,
    jumpTo: Option[Int],
    preNote: Option[String]
)

/** Игровое поле W x H. n — количество стартовых случайных клеток. */
class Pole(n: Int, val W: Int, val H: Int):
  import Pole.*
  require(n >= 0 && n <= StartGenes.length, s"n должно быть в диапазоне 0..${StartGenes.length}, получено $n")

  // ── Индекс непустых клеток (живых + трупов) ────────────────────────
  // Полный обход поля W×H дважды за тик — главный расход (память: блуждание
  // по разбросанным объектам Kletka). Индекс — компактный BitSet по сетке
  // (бит p = i*H + j взведён, если клетка НЕПУСТАЯ: живая или труп). Это
  // позволяет за тик трогать только непустые клетки в ТОМ ЖЕ порядке, что и
  // старый [[itr]] (i — внешний, j — внутренний; ключи p возрастают), через
  // nextSetBit. Клетки, родившиеся/переехавшие «впереди» курсора, так же
  // подхватываются в этот же тик — поэтому поведение совпадает с [[itr]]
  // 1-в-1. Трупы держим в индексе, чтобы каждый тик «тлеть» их (ageCorpse);
  // они исчезают по таймеру, так что не копятся бесконечно.
  // BitSet без боксинга — нет GC-нагрузки.
  private val occ: java.util.BitSet = new java.util.BitSet(W * H)

  // Старый itrobn сбрасывал флаги ВСЕХ клеток, в т.ч. пустых. Флаг `active`
  // пустой клетки читается как предусловие деления в неё ([[Kletka.burn]]).
  // Поэтому клетки, ОПУСТЕВШИЕ (del) в этот тик, запоминаем и сбрасываем им
  // флаги РОВНО в начале следующего тика (как это делал itrobn по всему полю).
  private val vacated = new scala.collection.mutable.ArrayBuffer[Int]()

  val matr: Array[Array[Kletka]] = Array.tabulate(W, H) { (i, j) =>
    val k = new Kletka
    k.px = i
    k.py = j
    k.tracker = (x, y, code) =>
      val p = x * H + y
      // Живые и трупы — в индексе; пустые выходят и помечаются на сброс флагов.
      if code == Kletka.StEmpty then
        occ.clear(p)
        vacated += p
      else occ.set(p)
    k
  }

  var leto: Boolean = true

  /** Сколько тиков осталось до смены текущего сезона. */
  private var seasonLeft: Int = SummerTicks

  /** Сколько тиков осталось до следующего смещения «гравитацией» вниз. */
  private var gravityLeft: Int = GravityTicks

  locally {
    var o = 0
    while o < n do
      var rw = Rng.int(W - 1)
      var rh = Rng.int((H - 1) / 2)
      while matr(rw)(rh).isLive do
        rw = Rng.int(W - 1)
        rh = Rng.int(H - 1)
      matr(rw)(rh).reviv(StartGenes(o))
      matr(rw)(rh).energy = StartEnergy
      o += 1
  }

  def itrobn(): Unit =
    for i <- 0 until W; j <- 0 until H do matr(i)(j).itnew()

  /**
   * Обработка одной клетки (i, j): проверка смерти, принудительное деление
   * при переизбытке энергии и выполнение одной команды генома. Вынесено из
   * [[itr]], чтобы переиспользовать в быстром индексированном [[itrFast]].
   */
  private def processCell(i: Int, j: Int): Unit =
    val cell = matr(i)(j)
    // Трупы постепенно «тлеют» и исчезают через заданное число тиков.
    if cell.isCorpse then cell.ageCorpse()
    if cell.isLive then
      if cell.timeLive <= 0 then cell.dead()
      if cell.energy <= 0 then cell.dead()
      if cell.energy >= ReproduceThreshold then
        val dir = bernvzmzn(i, j)
        if dir != -1 then
          cell.energy -= ForcedReproduceCost
          bern(i, j, dir)
        else cell.dead()

    // Guard: пропускаем неактивные/мёртвые клетки и пустой геном
    // (защита от деления на ноль).
    if cell.active && cell.isLive && cell.gen.nonEmpty then
      cell.timeLive -= 1
      val g = cell.cont % cell.gen.length
      cell.gen.charAt(g) match
        case 'f' =>
          val light = if leto then SummerLight else WinterLight
          val gain = (H - j) * light / H
          cell.energy += gain
          cell.fedLight += gain
          cell.cont += 1
          cell.active = false

        case 's' =>
          dirArg(cell, g) match
            case Some(t) if cell.energy - EnergyForStep >= 0 =>
              cell.active = false
              step(i, j, t)
            case _ => cell.bPerehod('s')

        case 'e' =>
          dirArg(cell, g) match
            case Some(t) if cell.energy - EnergyForDel >= 0 =>
              cell.active = false
              bern(i, j, t)
            case _ => cell.bPerehod('e')

        case 'a' =>
          dirArg(cell, g) match
            case Some(t) =>
              cell.active = false
              atack(i, j, t)
            case None => cell.bPerehod('a')

        case other => cell.bPerehod(other)

  /** Один шаг симуляции: обработка команд генома каждой живой клетки. */
  def itr(): Unit =
    itrobn()
    for i <- 0 until W; j <- 0 until H do processCell(i, j)

  /**
   * Быстрый шаг: трогаем только непустые клетки через упорядоченный индекс
   * [[live]], не сканируя пустоту. Обход идёт в том же порядке, что и
   * [[itr]] (ключ i*H+j возрастает), а клетки, родившиеся/переехавшие
   * «впереди» курсора, дообрабатываются в этот же тик через `higher` —
   * поэтому поведение совпадает с [[itr]] 1-в-1. Однопоточный и
   * детерминированный при фиксированном seed.
   */
  def itrFast(): Unit =
    // 0) Сброс флагов клеток, опустевших в ПРОШЛЫЙ тик (как делал itrobn по
    //    всему полю) — иначе `active` стал бы устаревшим для деления в них.
    //    Один буфер => ровно один тик задержки. Чистим до обработки, чтобы
    //    наполнить его опустевшими уже в этот тик.
    var k = 0
    while k < vacated.length do
      val c = vacated(k)
      if !occ.get(c) then matr(c / H)(c % H).itnew()
      k += 1
    vacated.clear()

    // 1) Сброс флагов всех непустых клеток (аналог itrobn, но без пустоты).
    var c = occ.nextSetBit(0)
    while c >= 0 do
      matr(c / H)(c % H).itnew()
      c = occ.nextSetBit(c + 1)

    // 2) Обработка в возрастающем порядке ключей; nextSetBit подхватывает
    //    клетки, добавленные по ходу тика (рождение/переезд вперёд курсора).
    c = occ.nextSetBit(0)
    while c >= 0 do
      processCell(c / H, c % H)
      c = occ.nextSetBit(c + 1)

  /**
   * Направление действия s/e/a — цифра 1..8, стоящая сразу после команды в
   * геноме. Берётся напрямую как номер направления (1=вверх … 8=вверх-влево),
   * без всякого «хеширования». Если следующего символа нет или он не цифра
   * 1..8 — направления нет (`None`), команда не выполняется и указатель прыгает
   * по `bPerehod`.
   */
  private def dirArg(cell: Kletka, g: Int): Option[Int] =
    if g + 1 < cell.gen.length then
      val c = cell.gen.charAt(g + 1)
      Option.when(c >= '1' && c <= '8')(c - '0')
    else None

  /** Координаты соседа в направлении t (1..8) с учётом границ поля. */
  private def neighbor(kx: Int, ky: Int, t: Int): Option[(Int, Int)] = t match
    case 1 => Option.when(ky >= 1)((kx, ky - 1))
    case 2 => Option.when(kx + 1 < W && ky >= 1)((kx + 1, ky - 1))
    case 3 => Option.when(kx + 1 < W)((kx + 1, ky))
    case 4 => Option.when(kx + 1 < W && ky < H - 1)((kx + 1, ky + 1))
    case 5 => Option.when(ky < H - 1)((kx, ky + 1))
    case 6 => Option.when(kx - 1 >= 0 && ky < H - 1)((kx - 1, ky + 1))
    case 7 => Option.when(kx - 1 >= 0)((kx - 1, ky))
    case 8 => Option.when(kx - 1 >= 0 && ky >= 1)((kx - 1, ky - 1))
    case _ => None

  private def occupied(tx: Int, ty: Int): Boolean =
    matr(tx)(ty).isLive || matr(tx)(ty).isCorpse

  private def atack(kx: Int, ky: Int, t: Int): Unit =
    val self = matr(kx)(ky)
    self.cont += t
    neighbor(kx, ky, t).filter((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      self.energy -= EnergyForStep
      if self.energy > 0 then matr(tx)(ty).atack(self)
      else self.del()
    }

  private def bern(kx: Int, ky: Int, t: Int): Unit =
    val self = matr(kx)(ky)
    self.cont += t
    neighbor(kx, ky, t).filterNot((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      self.energy -= EnergyForDel
      if self.energy > 0 then matr(tx)(ty).burn(self)
      else self.dead()
    }

  /** Перемещение клетки в одну из 8 соседних свободных ячеек. */
  private def step(kx: Int, ky: Int, t: Int): Unit =
    val self = matr(kx)(ky)
    self.cont += t
    neighbor(kx, ky, t).filterNot((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      self.energy -= EnergyForStep
      if self.energy > 0 then matr(tx)(ty).repl(self)
      else self.dead()
    }

  /** Случайное свободное соседнее направление для размножения, иначе -1. */
  private def bernvzmzn(kx: Int, ky: Int): Int =
    val free = Bervz.filter(d => neighbor(kx, ky, d).exists((tx, ty) => !occupied(tx, ty)))
    if free.isEmpty then -1 else free(Rng.int(free.length))

  /** Куда уйдёт указатель при «прыжке» bPerehod по символу s (без мутации). */
  private def jumpTargetOf(k: Kletka, s: Char): Int =
    val len = k.gen.length
    val z = k.gen.indexOf(s.toInt)
    if z < 0 then k.cont % len
    else
      val next = (k.cont + z) % len
      if next != k.cont then next else (k.cont + z + 3) % len

  /**
   * Read-only расшифровка текущей команды клетки (x, y) для GenomeWindow.
   * Повторяет ветвление [[itr]], но без побочных эффектов.
   */
  def explain(x: Int, y: Int): StepExplanation =
    val k = matr(x)(y)
    val gen = k.gen
    if !k.isLive || gen.isEmpty then
      StepExplanation(0, ' ', GenAction.Idle, None, None, false, 0, 0, 0, None, None)
    else
      val len = gen.length
      val g = k.cont % len
      val sym = gen.charAt(g)
      val preNote =
        if k.timeLive <= 0 || k.energy <= 0 then Some("клетка погибнет в этот ход")
        else if k.energy >= ReproduceThreshold then
          Some("переизбыток энергии — будет принудительное деление")
        else None
      sym match
        case 'f' =>
          val light = if leto then SummerLight else WinterLight
          val gain = (H - y) * light / H
          StepExplanation(g, 'f', GenAction.Photosynthesis, None, None,
            feasible = true, energyCost = 0, energyGain = gain,
            nextPointer = (k.cont + 1) % len, jumpTo = None, preNote = preNote)

        case 's' =>
          dirArg(k, g) match
            case Some(t) =>
              val ready = neighbor(x, y, t).map((tx, ty) => !occupied(tx, ty))
              val feasible = k.energy - EnergyForStep >= 0
              val jump = jumpTargetOf(k, 's')
              val next = if feasible then (k.cont + t) % len else jump
              StepExplanation(g, 's', GenAction.Step, Some(t), ready, feasible,
                EnergyForStep, 0, next, if feasible then None else Some(jump), preNote)
            case None =>
              val jump = jumpTargetOf(k, 's')
              StepExplanation(g, 's', GenAction.Jump, None, None, false, 0, 0, jump, Some(jump), preNote)

        case 'e' =>
          dirArg(k, g) match
            case Some(t) =>
              val ready = neighbor(x, y, t).map((tx, ty) => !occupied(tx, ty))
              val feasible = k.energy - EnergyForDel >= 0
              val jump = jumpTargetOf(k, 'e')
              val next = if feasible then (k.cont + t) % len else jump
              StepExplanation(g, 'e', GenAction.Divide, Some(t), ready, feasible,
                EnergyForDel, 0, next, if feasible then None else Some(jump), preNote)
            case None =>
              val jump = jumpTargetOf(k, 'e')
              StepExplanation(g, 'e', GenAction.Jump, None, None, false, 0, 0, jump, Some(jump), preNote)

        case 'a' =>
          dirArg(k, g) match
            case Some(t) =>
              val ready = neighbor(x, y, t).map((tx, ty) => occupied(tx, ty))
              StepExplanation(g, 'a', GenAction.Attack, Some(t), ready,
                feasible = true, energyCost = EnergyForStep, energyGain = 0,
                nextPointer = (k.cont + t) % len, jumpTo = None, preNote = preNote)
            case None =>
              val jump = jumpTargetOf(k, 'a')
              StepExplanation(g, 'a', GenAction.Jump, None, None, false, 0, 0, jump, Some(jump), preNote)

        case other =>
          val jump = jumpTargetOf(k, other)
          StepExplanation(g, other, GenAction.Jump, None, None, false, 0, 0, jump, Some(jump), preNote)

  /** Клетка по координатам поля — для просмотра генома в отдельном окне. */
  def kletka(w: Int, h: Int): Kletka = matr(w)(h)

  /** Идентификатор клетки в ячейке (0 — пусто) — чтобы «привязаться» к клетке. */
  def idAt(w: Int, h: Int): Long = matr(w)(h).id

  /**
   * Найти текущие координаты клетки по её идентификатору.
   * Нужна, чтобы следить за конкретной клеткой, которая может перемещаться
   * по полю, а не за фиксированной ячейкой.
   */
  def findById(id: Long): Option[(Int, Int)] =
    if id == 0 then None
    else
      var i = 0
      while i < W do
        var j = 0
        while j < H do
          if matr(i)(j).id == id then return Some((i, j))
          j += 1
        i += 1
      None

  def getLive(w: Int, h: Int): Boolean = matr(w)(h).isLive
  def getDead(w: Int, h: Int): Boolean = matr(w)(h).isDead
  def isCorpse(w: Int, h: Int): Boolean = matr(w)(h).isCorpse
  def isBern(w: Int, h: Int): Boolean = matr(w)(h).isBorn

  /** Энергия клетки — для окраски яркостью. */
  def getEnergy(w: Int, h: Int): Int = matr(w)(h).energy

  /** Источники питания клетки (свет, живая добыча, трупы) — для окраски оттенком. */
  def getDiet(w: Int, h: Int): (Int, Int, Int) =
    val k = matr(w)(h)
    (k.fedLight, k.fedPrey, k.fedCorpse)

  /**
   * Освещённость (питательность среды) в строке h — совпадает с энергией,
   * которую там даёт фотосинтез. Максимум сверху (h=0), почти ноль снизу;
   * зимой в 4 раза меньше, чем летом.
   */
  def lightAt(h: Int): Int = (H - h) * (if leto then SummerLight else WinterLight) / H

  /** Максимально возможная освещённость (летний потолок) — для нормировки цвета. */
  def maxLight: Int = SummerLight

  /** Принудительная смена сезона (сбрасывает таймер на длительность нового). */
  def year(): Unit =
    leto = !leto
    seasonLeft = if leto then SummerTicks else WinterTicks

  /**
   * Тик сезонного таймера: уменьшает остаток текущего сезона и меняет
   * сезон, когда он истёк. Длительность лета и зимы задаётся независимо
   * ([[Pole.SummerTicks]] / [[Pole.WinterTicks]]).
   */
  def advanceSeason(): Unit =
    seasonLeft -= 1
    if seasonLeft <= 0 then year()

  /**
   * Тик «гравитации»: каждые [[Pole.GravityTicks]] тиков все непустые ячейки
   * (живые клетки и трупы) смещаются на одну позицию вниз, если место под
   * ними свободно. При значении 0 гравитация отключена.
   */
  def advanceGravity(): Unit =
    if GravityTicks > 0 then
      gravityLeft -= 1
      if gravityLeft <= 0 then
        gravityLeft = GravityTicks
        applyGravity()

  /**
   * Один шаг гравитации: сдвигает каждую непустую ячейку вниз на 1, если ниже
   * свободно. Обход снизу вверх (j от H-2 к 0), чтобы за один проход клетка
   * падала ровно на одну ячейку, а не «проваливалась» сразу на несколько.
   */
  private def applyGravity(): Unit =
    var j = H - 2
    while j >= 0 do
      var i = 0
      while i < W do
        val cell = matr(i)(j)
        if (cell.isLive || cell.isCorpse) && !occupied(i, j + 1) then
          cell.fallInto(matr(i)(j + 1))
        i += 1
      j -= 1
