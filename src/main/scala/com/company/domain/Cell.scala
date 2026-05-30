package com.company.domain

object Cell:
  /** Коды состояния для трекера индекса занятости (см. [[Cell.tracker]]). */
  inline val StEmpty = 0
  inline val StAlive = 1
  inline val StCorpse = 2

  /**
   * Счётчик уникальных идентификаторов клеток. Атомарный, потому что в
   * многопоточном режиме `freshId()` может вызываться из нескольких потоков.
   */
  private val idCounter = new java.util.concurrent.atomic.AtomicLong(0)
  private def freshId(): Long = idCounter.incrementAndGet()

/**
 * Одна клетка поля — Entity с идентичностью [[id]]. Хранит состояние
 * (геном, энергия, ресурс жизни, указатель, рацион) и реализует доменные
 * переходы. Решения, требующие конфигурации/ГСЧ (мутация, хищничество),
 * получают их параметром — конфиг больше не живёт в глобальном состоянии.
 *
 * Это «императивное» ядро аккуратно мутирует свои поля; чистые расчёты
 * (декод команды, мутация, родство) делегируются [[Genome]].
 */
final class Cell extends CellView:
  import Cell.*

  private var state: CellState = CellState.Empty
  /** Готова ли клетка действовать в этот ход. */
  var active: Boolean = true
  private var bornFlag: Boolean = true
  private var diedFlag: Boolean = false

  private var gen: Genome = Genome.Empty
  private var en: Int = 1
  private var life: Int = 1000
  private var ptr: Int = 0
  private var corpseAge: Int = 0
  private var cellId: Long = 0
  private var feeding: Diet = Diet.Empty

  // ── Привязка к ячейке поля и индексу занятости (бухгалтерия агрегата). ──
  var px: Int = -1
  var py: Int = -1
  var tracker: (Int, Int, Int) => Unit = (_, _, _) => ()
  private inline def track(stateCode: Int): Unit = tracker(px, py, stateCode)

  // ── CellView ──
  def isAlive: Boolean = state == CellState.Alive
  def isCorpse: Boolean = state == CellState.Corpse
  def diedThisTick: Boolean = diedFlag
  def bornThisTick: Boolean = bornFlag
  def energy: Int = en
  def lifeLeft: Int = life
  def genome: Genome = gen
  def pointer: Int = ptr
  def diet: Diet = feeding
  def id: Long = cellId

  // ── мелкие мутаторы для оркестрации в Field ──
  def addEnergy(n: Int): Unit = en += n
  def setEnergy(n: Int): Unit = en = n
  def advancePointer(n: Int): Unit = ptr += n
  def deactivate(): Unit = active = false
  def tickLife(): Unit = life -= 1
  def setGenome(g: Genome): Unit = gen = g

  /**
   * Ручная замена генома (правка из UI): ставит новый геном и сбрасывает
   * указатель в начало, чтобы клетка исполняла программу заново и указатель
   * гарантированно был валиден для новой длины.
   */
  def rewriteGenome(g: Genome): Unit =
    gen = g
    ptr = 0

  /** Накопить энергию от фотосинтеза (для окраски «фотосинтетик»). */
  def feedLight(gain: Int): Unit =
    en += gain
    feeding = feeding.addLight(gain)

  /** Переход указателя генома при невозможности выполнить команду. */
  def jump(sym: Char): Unit =
    active = false
    en -= 1
    ptr = gen.jumpTarget(ptr, sym)

  /** Оживить клетку с заданным геномом. */
  def revive(startGen: Genome): Unit =
    state = CellState.Alive
    en = 5
    gen = startGen
    feeding = Diet.LightSeed
    cellId = freshId()
    track(StAlive)

  /** Подготовка к новому ходу: сброс одноразовых флагов. */
  def beginTick(): Unit =
    active = true
    diedFlag = false
    bornFlag = false

  /**
   * Гравитация: переместить содержимое этой ячейки (живой клетки ИЛИ трупа)
   * в `dst` без изменения состояния и полей. Текущая ячейка пустеет.
   */
  def fallInto(dst: Cell): Unit =
    dst.state = state
    dst.active = active
    dst.bornFlag = bornFlag
    dst.diedFlag = diedFlag
    dst.gen = gen
    dst.en = en
    dst.life = life
    dst.ptr = ptr
    dst.corpseAge = corpseAge
    dst.cellId = cellId
    dst.feeding = feeding
    dst.track(if state == CellState.Alive then StAlive else StCorpse)
    clear()

  /** Перемещение: клетка-источник `src` переезжает в эту ячейку. */
  def moveInto(src: Cell): Unit =
    gen = src.gen
    active = src.active
    state = CellState.Alive
    diedFlag = false
    ptr = src.ptr
    en += src.en
    // Мигрант несёт СВОЙ ресурс жизни (как в fallInto). Иначе клетка
    // наследовала бы lifeLeft ячейки-приёмника — и, съев труп умершего от
    // старости (lifeLeft <= 0), сразу гибла бы на следующем тике.
    life = src.life
    feeding = src.feeding
    // Та же самая клетка переехала — сохраняем идентификатор, чтобы окно
    // генома продолжало следить именно за ней.
    cellId = src.cellId
    track(StAlive)
    src.clear()

  /** Размножение: потомок наследует геном родителя (с шансом мутации). */
  def reproduce(parent: Cell, rng: Rng, cfg: SimulationConfig): Unit =
    if parent.active && active then
      ptr = 0
      cellId = freshId()
      gen = parent.gen
      if rng.nextInt(cfg.mutationChance) == 1 then gen = gen.mutate(rng, cfg)
      else active = parent.active
      state = CellState.Alive
      diedFlag = false
      en += parent.en / 3
      bornFlag = true
      parent.en -= en
      // Унаследовать «склонность к питанию» родителя в нормированном виде.
      feeding = parent.feeding.normalized(100)
      track(StAlive)

  /** Смерть: клетка становится трупом, сохраняя энергию для падальщиков. */
  def die(): Unit =
    ptr = 0
    active = false
    state = CellState.Corpse
    diedFlag = true
    gen = Genome.Empty
    en = math.max(en, 0)
    corpseAge = 0
    track(StCorpse)

  /**
   * «Тление» трупа: каждый тик увеличивает возраст; по достижении
   * `cfg.corpseDecayTicks` труп исчезает. При отрицательном значении — никогда.
   */
  def ageCorpse(cfg: SimulationConfig): Unit =
    if cfg.corpseDecayTicks >= 0 then
      corpseAge += 1
      if corpseAge >= cfg.corpseDecayTicks then clear()

  /** Опустошить ячейку. */
  def clear(): Unit =
    ptr = 0
    active = false
    state = CellState.Empty
    diedFlag = false
    gen = Genome.Empty
    en = 1
    life = 100
    corpseAge = 0
    cellId = 0
    track(StEmpty)

  /** Поглощение этой клетки/трупа атакующим (`gain` достаётся ему в moveInto). */
  private def devour(attacker: Cell, gain: Int, prey: Boolean): Unit =
    en = gain
    if prey then attacker.feeding = attacker.feeding.addPrey(gain)
    else attacker.feeding = attacker.feeding.addCorpse(gain)
    moveInto(attacker)

  /** Атака: `attacker` нападает на эту клетку/труп. */
  def attackedBy(attacker: Cell, cfg: SimulationConfig): Unit =
    if !isCorpse then
      if !gen.isKin(attacker.gen, cfg.kinshipThresholdPct) then
        if attacker.en - cfg.preyAdvantage > en then
          // Хищник: энергия добычи, но не меньше «бонуса за добычу».
          devour(attacker, math.max(en, cfg.weakPreyBonus), prey = true)
        else
          attacker.en -= cfg.retribution
          if attacker.en <= 0 then attacker.die()
          else attacker.ptr += 1
    else
      // Падальщик: энергия трупа, но не меньше «бонуса за труп».
      devour(attacker, math.max(en, cfg.corpseBonus), prey = false)
