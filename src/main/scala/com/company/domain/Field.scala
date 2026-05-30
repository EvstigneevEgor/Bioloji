package com.company.domain

object Field:
  /** Стартовые геномы для случайных клеток. */
  val StartGenes: Array[String] =
    Array("fffffffffffffffffffff", "ffffffffffffffff", "f", "f", "f", "f", "f", "f")

  /** Направления для принудительного деления при переизбытке энергии. */
  private val ForcedDirs = Array(1, 3, 5, 7)

/**
 * Игровое поле W×H — Aggregate Root симуляции и «императивная оболочка»:
 * мутабельная сетка клеток, индекс занятости и таймеры сезона/гравитации.
 * Чистые решения (декод команды, мутация, родство) делегируются [[Genome]];
 * здесь — применение эффектов к сетке.
 *
 * @param n          количество стартовых случайных клеток (0..StartGenes.length)
 * @param initialCfg конфигурация на момент создания
 * @param rng        инжектируемый ГСЧ
 */
class Field(n: Int, val W: Int, val H: Int, initialCfg: SimulationConfig, rng: Rng) extends WorldView:
  import Field.*

  require(
    n >= 0 && n <= StartGenes.length,
    s"n должно быть в диапазоне 0..${StartGenes.length}, получено $n"
  )

  /**
   * Текущая конфигурация. Иммутабельное значение, которое приложение может
   * атомарно заменить «на лету» (живая настройка параметров без пересоздания
   * поля). Внутри тика читается консистентно.
   */
  var config: SimulationConfig = initialCfg

  // ── Индекс непустых клеток (живых + трупов) ────────────────────────
  // BitSet по сетке (бит p = i*H + j взведён, если клетка НЕПУСТАЯ). Позволяет
  // за тик трогать только непустые клетки в том же порядке, что и полный обход.
  private val occ: java.util.BitSet = new java.util.BitSet(W * H)

  // Клетки, ОПУСТЕВШИЕ (clear) в этот тик: им сбрасываем флаги ровно в начале
  // следующего тика (как делал полный обход по всему полю).
  private val vacated = new scala.collection.mutable.ArrayBuffer[Int]()

  private val grid: Array[Array[Cell]] = Array.tabulate(W, H) { (i, j) =>
    val k = new Cell
    k.px = i
    k.py = j
    k.tracker = (x, y, code) =>
      val p = x * H + y
      if code == Cell.StEmpty then
        occ.clear(p)
        vacated += p
      else occ.set(p)
    k
  }

  private var summer: Boolean = true
  private var seasonLeft: Int = initialCfg.summerTicks
  private var gravityLeft: Int = initialCfg.gravityTicks

  locally {
    var o = 0
    while o < n do
      var rw = rng.nextInt(W - 1)
      var rh = rng.nextInt((H - 1) / 2)
      while grid(rw)(rh).isAlive do
        rw = rng.nextInt(W - 1)
        rh = rng.nextInt(H - 1)
      grid(rw)(rh).revive(Genome(StartGenes(o)))
      grid(rw)(rh).setEnergy(initialCfg.startEnergy)
      o += 1
  }

  // ── WorldView ──
  def isSummer: Boolean = summer
  def cell(x: Int, y: Int): CellView = grid(x)(y)
  def idAt(x: Int, y: Int): Long = grid(x)(y).id

  def findById(id: Long): Option[(Int, Int)] =
    if id == 0 then None
    else
      var i = 0
      while i < W do
        var j = 0
        while j < H do
          if grid(i)(j).id == id then return Some((i, j))
          j += 1
        i += 1
      None

  def lightAt(h: Int): Int =
    (H - h) * (if summer then config.summerLight else config.winterLight) / H

  def maxLight: Int = config.summerLight

  /**
   * Ручная замена генома живой клетки в ячейке (x, y) — для редактирования из
   * UI. Применяется только к живой клетке (у трупов/пустых ячеек генома нет).
   * Вызывается слоем приложения под внешней синхронизацией с тиком.
   *
   * @return true, если клетка была живой и геном заменён.
   */
  def setGenomeAt(x: Int, y: Int, code: String): Boolean =
    if !inBounds(x, y) then false
    else
      val c = grid(x)(y)
      if c.isAlive then
        c.rewriteGenome(Genome(Genome.sanitize(code)))
        true
      else false

  private inline def inBounds(x: Int, y: Int): Boolean =
    x >= 0 && y >= 0 && x < W && y < H

  /**
   * Служебный ввод состояния (для тестов и потенциальной «посадки» из UI):
   * оживить клетку в (x, y) с заданными геномом и энергией. Указатель — в
   * начало. Возвращает false, если координаты вне поля.
   */
  def reviveAt(x: Int, y: Int, code: String, energy: Int): Boolean =
    if !inBounds(x, y) then false
    else
      val c = grid(x)(y)
      c.revive(Genome(Genome.sanitize(code)))
      c.setEnergy(energy)
      true

  /**
   * Служебный ввод состояния (для тестов): поставить труп в (x, y) с заданной
   * энергией. При `oldAge = true` труп «умер от старости» — его остаток ресурса
   * жизни ≤ 0 (именно такие трупы вскрывают перенос `lifeLeft` при поедании).
   * Возвращает false, если координаты вне поля.
   */
  def placeCorpseAt(x: Int, y: Int, energy: Int, oldAge: Boolean = false): Boolean =
    if !inBounds(x, y) then false
    else
      val c = grid(x)(y)
      c.revive(Genome("f"))
      c.setEnergy(energy)
      if oldAge then while c.lifeLeft > 0 do c.tickLife()
      c.die()
      true

  // ── шаг симуляции ──
  /** Сброс одноразовых флагов всех клеток. */
  def resetFlags(): Unit =
    for i <- 0 until W; j <- 0 until H do grid(i)(j).beginTick()

  /**
   * Обработка одной клетки (i, j): тление трупа, проверка смерти,
   * принудительное деление при переизбытке энергии и выполнение одной команды.
   */
  private def processCell(i: Int, j: Int): Unit =
    val cfg = config
    val c = grid(i)(j)
    if c.isCorpse then c.ageCorpse(cfg)
    if c.isAlive then
      if c.lifeLeft <= 0 then c.die()
      if c.energy <= 0 then c.die()
      if c.energy >= cfg.reproduceThreshold then
        val dir = freeForcedDir(i, j)
        if dir != -1 then
          c.addEnergy(-cfg.forcedReproduceCost)
          divide(i, j, dir)
        else c.die()

    if c.active && c.isAlive && c.genome.nonEmpty then
      c.tickLife()
      c.genome.commandAt(c.pointer) match
        case Command.Photosynthesis =>
          val light = if summer then cfg.summerLight else cfg.winterLight
          val gain = (H - j) * light / H
          c.feedLight(gain)
          c.advancePointer(1)
          c.deactivate()

        case Command.Step(t) =>
          if c.energy - cfg.energyForStep >= 0 then
            c.deactivate()
            step(i, j, t)
          else c.jump('s')

        case Command.Divide(t) =>
          if c.energy - cfg.energyForDel >= 0 then
            // Делим, пока клетка активна: reproduce требует active-родителя
            // (как в принудительном делении). Родитель не двигается, поэтому
            // деактивируем уже ПОСЛЕ — повторно в этот тик он не обрабатывается.
            divide(i, j, t)
            c.deactivate()
          else c.jump('e')

        case Command.Attack(t) =>
          c.deactivate()
          attack(i, j, t)

        case Command.Scavenge(t) =>
          c.deactivate()
          scavenge(i, j, t)

        case Command.Jump(sym) => c.jump(sym)
        case Command.Idle      => ()

  /** Один шаг симуляции полным обходом (эталонный путь). */
  def tick(): Unit =
    resetFlags()
    for i <- 0 until W; j <- 0 until H do processCell(i, j)

  /**
   * Быстрый шаг: трогаем только непустые клетки через упорядоченный индекс,
   * не сканируя пустоту. Обход в том же порядке, что и [[tick]]; должен давать
   * идентичный результат (это проверяет тест-инвариант).
   */
  def tickFast(): Unit =
    // 0) Сброс флагов клеток, опустевших в ПРОШЛЫЙ тик.
    var k = 0
    while k < vacated.length do
      val p = vacated(k)
      if !occ.get(p) then grid(p / H)(p % H).beginTick()
      k += 1
    vacated.clear()

    // 1) Сброс флагов всех непустых клеток.
    var c = occ.nextSetBit(0)
    while c >= 0 do
      grid(c / H)(c % H).beginTick()
      c = occ.nextSetBit(c + 1)

    // 2) Обработка в возрастающем порядке ключей.
    c = occ.nextSetBit(0)
    while c >= 0 do
      processCell(c / H, c % H)
      c = occ.nextSetBit(c + 1)

  // ── геометрия и действия ──
  /** Координаты соседа в направлении t (1..8) с учётом границ поля. */
  private def neighbor(kx: Int, ky: Int, t: Int): Option[(Int, Int)] =
    if t < 1 || t > 8 then None
    else
      val (dx, dy) = Direction.offset(t)
      val nx = kx + dx
      val ny = ky + dy
      Option.when(nx >= 0 && nx < W && ny >= 0 && ny < H)((nx, ny))

  private def occupied(tx: Int, ty: Int): Boolean =
    grid(tx)(ty).isAlive || grid(tx)(ty).isCorpse

  private def attack(kx: Int, ky: Int, t: Int): Unit =
    val self = grid(kx)(ky)
    self.advancePointer(2)
    neighbor(kx, ky, t).filter((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      val target = grid(tx)(ty)
      // Удар по живой РОДНЕ — «пустой» (attackedBy ничего не делает), поэтому
      // энергию за него НЕ списываем: иначе скученный лайнедж разорял бы сам
      // себя. Трупы и чужаки — настоящее взаимодействие, за них платим.
      val interacts =
        target.isCorpse || !target.genome.isKin(self.genome, config.kinshipThresholdPct)
      if interacts then
        self.addEnergy(-config.energyForStep)
        if self.energy > 0 then target.attackedBy(self, config)
        else self.clear()
    }

  /**
   * Падальщик `c`: поедает труп за `scavengeCost` (дешевле атаки). В отличие от
   * слепых s/e/a, падальщик ИЩЕТ падаль: предпочитает направление t, но если
   * там трупа нет — берёт любой соседний труп (детерминированный обход 1..8).
   * Если трупов рядом нет — «промах» без затрат энергии.
   */
  private def scavenge(kx: Int, ky: Int, t: Int): Unit =
    val self = grid(kx)(ky)
    self.advancePointer(2)
    scavengeTarget(kx, ky, t).foreach { (tx, ty) =>
      self.addEnergy(-config.scavengeCost)
      if self.energy > 0 then grid(tx)(ty).attackedBy(self, config)
      else self.clear()
    }

  /**
   * Цель падальщика: труп в предпочитаемом направлении t, иначе первый труп
   * при обходе направлений 1..8 (детерминированно, без ГСЧ — чтобы tickFast
   * совпадал с tick). None — соседних трупов нет.
   */
  private def scavengeTarget(kx: Int, ky: Int, t: Int): Option[(Int, Int)] =
    def corpseAt(d: Int): Option[(Int, Int)] =
      neighbor(kx, ky, d).filter((tx, ty) => grid(tx)(ty).isCorpse)
    corpseAt(t).orElse((1 to 8).iterator.flatMap(corpseAt).nextOption())

  private def divide(kx: Int, ky: Int, t: Int): Unit =
    val self = grid(kx)(ky)
    self.advancePointer(2)
    neighbor(kx, ky, t).filterNot((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      self.addEnergy(-config.energyForDel)
      if self.energy > 0 then grid(tx)(ty).reproduce(self, rng, config)
      else self.die()
    }

  /** Перемещение клетки в одну из 8 соседних свободных ячеек. */
  private def step(kx: Int, ky: Int, t: Int): Unit =
    val self = grid(kx)(ky)
    self.advancePointer(2)
    neighbor(kx, ky, t).filterNot((tx, ty) => occupied(tx, ty)).foreach { (tx, ty) =>
      self.addEnergy(-config.energyForStep)
      if self.energy > 0 then grid(tx)(ty).moveInto(self)
      else self.die()
    }

  /** Случайное свободное соседнее направление для размножения, иначе -1. */
  private def freeForcedDir(kx: Int, ky: Int): Int =
    val free = ForcedDirs.filter(d => neighbor(kx, ky, d).exists((tx, ty) => !occupied(tx, ty)))
    if free.isEmpty then -1 else free(rng.nextInt(free.length))

  /**
   * Read-only расшифровка текущей команды клетки (x, y) для GenomeWindow.
   * Повторяет ветвление [[processCell]], но без побочных эффектов.
   */
  def explain(x: Int, y: Int): StepExplanation =
    val cfg = config
    val c = grid(x)(y)
    val g = c.genome
    if !c.isAlive || g.isEmpty then StepExplanation.Idle
    else
      val len = g.length
      val ptr = c.pointer
      val gi = ptr % len
      val preNote =
        if c.lifeLeft <= 0 || c.energy <= 0 then Some("клетка погибнет в этот ход")
        else if c.energy >= cfg.reproduceThreshold then
          Some("переизбыток энергии — будет принудительное деление")
        else None
      g.commandAt(ptr) match
        case Command.Photosynthesis =>
          val light = if summer then cfg.summerLight else cfg.winterLight
          val gain = (H - y) * light / H
          StepExplanation(gi, 'f', GenAction.Photosynthesis, None, None,
            feasible = true, energyCost = 0, energyGain = gain,
            nextPointer = (ptr + 1) % len, jumpTo = None, preNote = preNote)

        case Command.Step(t) =>
          val ready = neighbor(x, y, t).map((tx, ty) => !occupied(tx, ty))
          val feasible = c.energy - cfg.energyForStep >= 0
          val jump = g.jumpTarget(ptr, 's')
          val next = if feasible then (ptr + 2) % len else jump
          StepExplanation(gi, 's', GenAction.Step, Some(t), ready, feasible,
            cfg.energyForStep, 0, next, if feasible then None else Some(jump), preNote)

        case Command.Divide(t) =>
          val ready = neighbor(x, y, t).map((tx, ty) => !occupied(tx, ty))
          val feasible = c.energy - cfg.energyForDel >= 0
          val jump = g.jumpTarget(ptr, 'e')
          val next = if feasible then (ptr + 2) % len else jump
          StepExplanation(gi, 'e', GenAction.Divide, Some(t), ready, feasible,
            cfg.energyForDel, 0, next, if feasible then None else Some(jump), preNote)

        case Command.Attack(t) =>
          val ready = neighbor(x, y, t).map((tx, ty) => occupied(tx, ty))
          StepExplanation(gi, 'a', GenAction.Attack, Some(t), ready,
            feasible = true, energyCost = cfg.energyForStep, energyGain = 0,
            nextPointer = (ptr + 2) % len, jumpTo = None, preNote = preNote)

        case Command.Scavenge(t) =>
          // Падальщик «готов», если рядом есть труп (в направлении t или любой).
          val ready = Some(scavengeTarget(x, y, t).isDefined)
          StepExplanation(gi, 'c', GenAction.Scavenge, Some(t), ready,
            feasible = true, energyCost = cfg.scavengeCost, energyGain = 0,
            nextPointer = (ptr + 2) % len, jumpTo = None, preNote = preNote)

        case Command.Jump(sym) =>
          val jump = g.jumpTarget(ptr, sym)
          StepExplanation(gi, sym, GenAction.Jump, None, None, false, 0, 0, jump, Some(jump), preNote)

        case Command.Idle => StepExplanation.Idle

  // ── сезоны и гравитация ──
  /** Принудительная смена сезона (сбрасывает таймер на длительность нового). */
  def toggleSeason(): Unit =
    summer = !summer
    seasonLeft = if summer then config.summerTicks else config.winterTicks

  /** Тик сезонного таймера: меняет сезон, когда текущий истёк. */
  def advanceSeason(): Unit =
    seasonLeft -= 1
    if seasonLeft <= 0 then toggleSeason()

  /** Тик «гравитации»: периодически смещает всё непустое вниз на 1 ячейку. */
  def advanceGravity(): Unit =
    if config.gravityTicks > 0 then
      gravityLeft -= 1
      if gravityLeft <= 0 then
        gravityLeft = config.gravityTicks
        applyGravity()

  /**
   * Один шаг гравитации: сдвигает каждую непустую ячейку вниз на 1, если ниже
   * свободно. Обход снизу вверх — за проход клетка падает ровно на одну ячейку.
   */
  private def applyGravity(): Unit =
    var j = H - 2
    while j >= 0 do
      var i = 0
      while i < W do
        val c = grid(i)(j)
        if (c.isAlive || c.isCorpse) && !occupied(i, j + 1) then
          c.fallInto(grid(i)(j + 1))
        i += 1
      j -= 1
