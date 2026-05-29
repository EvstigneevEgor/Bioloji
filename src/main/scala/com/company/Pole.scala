package com.company

object Pole:
  private val EnergyForDel = 150
  private val EnergyForStep = 250
  /** Порог энергии для принудительного деления и его стоимость. */
  private val ReproduceThreshold = EnergyForDel * 50
  private val ForcedReproduceCost = EnergyForDel * 40
  /** Энергия стартовых клеток. */
  private val StartEnergy = 150
  /** Стартовые геномы для случайных клеток. */
  private val StartGenes =
    Array("fffffffffffffffffffff", "ffffffffffffffff", "f", "f", "f", "f", "f", "f")
  /** Допустимые направления для размножения/атаки. */
  private val Bervz = Array(1, 3, 5, 7)
  /** Множители фотосинтеза летом/зимой: energy += (H - j) * mult / H. */
  private val SummerLight = 100
  private val WinterLight = 25

/** Игровое поле W x H. n — количество стартовых случайных клеток. */
class Pole(n: Int, val W: Int, val H: Int):
  import Pole.*
  require(n >= 0 && n <= StartGenes.length, s"n должно быть в диапазоне 0..${StartGenes.length}, получено $n")

  val matr: Array[Array[Kletka]] = Array.tabulate(W, H)((_, _) => new Kletka)

  var leto: Boolean = true

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

  /** Один шаг симуляции: обработка команд генома каждой живой клетки. */
  def itr(): Unit =
    itrobn()
    for i <- 0 until W; j <- 0 until H do
      val cell = matr(i)(j)
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
            if g + 1 < cell.gen.length then
              val t = cell.hash(g + 1) % 9 + 1
              if cell.energy - EnergyForStep >= 0 then
                cell.active = false
                step(i, j, t)
              else cell.bPerehod('s')
            else cell.bPerehod('s')

          case 'e' =>
            if g + 1 < cell.gen.length then
              val t = Bervz(cell.hash(g + 1) % 4)
              if cell.energy - EnergyForDel >= 0 then
                cell.active = false
                bern(i, j, t)
              else cell.bPerehod('e')
            else cell.bPerehod('e')

          case 'a' =>
            if g + 1 < cell.gen.length then
              val t = Bervz(cell.hash(g + 1) % 4)
              cell.active = false
              atack(i, j, t)
            else cell.bPerehod('a')

          case other => cell.bPerehod(other)

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

  /** Клетка по координатам поля — для просмотра генома в отдельном окне. */
  def kletka(w: Int, h: Int): Kletka = matr(w)(h)

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

  def year(): Unit = leto = !leto
