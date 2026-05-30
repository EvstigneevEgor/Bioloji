package com.company

import com.company.domain.*

/**
 * Поведение «успешной» клетки-падальщика-фотосинтетика в разных окружениях.
 *
 * Геном такой клетки: фотосинтез ради базовой энергии + команда `c` (падальщик),
 * которая ищет труп среди соседей и съедает его, переезжая на его место.
 *
 * Отдельно проверяется регрессия: раньше клетка «умирала после первого
 * поедания трупа» — при поедании [[Cell.moveInto]] не переносил `lifeLeft`
 * мигранта, и клетка наследовала нулевой ресурс жизни трупа (умершего от
 * старости), после чего сразу гибла на проверке `lifeLeft <= 0`.
 */
class ScavengerCellTest extends munit.FunSuite:

  private val cfg = SimulationConfig.Default

  // Две версии генома из обсуждения: сбалансированная и «упор на падаль».
  private val Balanced  = "ffffffc1e2c3e4"
  private val Scavenger = "ffffc1c3e2c5c7e4"

  /** Перепись поля: (число живых, число трупов). */
  private def census(f: Field): (Int, Int) =
    var live = 0
    var dead = 0
    for i <- 0 until f.W; j <- 0 until f.H do
      if f.cell(i, j).isAlive then live += 1
      else if f.cell(i, j).isCorpse then dead += 1
    (live, dead)

  // ── корневая причина (Cell) ───────────────────────────────
  test("Падальщик сохраняет СВОЙ ресурс жизни, съев труп умершего от старости") {
    // Труп, умерший от истощения ресурса жизни (lifeLeft <= 0).
    val corpse = new Cell
    corpse.revive(Genome("f"))
    while corpse.lifeLeft > 0 do corpse.tickLife()
    corpse.die()
    assert(corpse.isCorpse)
    assert(corpse.lifeLeft <= 0)

    val scav = new Cell
    scav.revive(Genome("c1"))
    scav.setEnergy(500)
    val lifeBefore = scav.lifeLeft
    assert(lifeBefore > 0)

    // Съел труп и «переехал» в его ячейку.
    corpse.attackedBy(scav, cfg)
    assert(corpse.isAlive, "ячейка трупа теперь живой падальщик")
    assertEquals(
      corpse.lifeLeft,
      lifeBefore,
      "клетка должна нести свой ресурс жизни, а не нулевой ресурс трупа"
    )
  }

  // ── 1. Пустое поле ────────────────────────────────────────
  for code <- List(Balanced, Scavenger) do
    test(s"Пустое поле: клетка [$code] выживает и размножается фотосинтезом") {
      val f = new Field(0, 40, 40, cfg, new SeededRng(1))
      assert(f.reviveAt(20, 1, code, 200)) // у поверхности — максимум света
      var maxLive = 0
      for _ <- 0 until 200 do
        f.tickFast()
        maxLive = math.max(maxLive, census(f)._1)
      val (live, _) = census(f)
      assert(maxLive >= 2, s"клетка должна размножиться, maxLive=$maxLive")
      assert(live >= 1, s"линия не должна вымереть, live=$live")
    }

  // ── 2. В окружении живой семьи (родня) ────────────────────
  test("Окружение семьи: падальщик не трогает родню (нет команды атаки)") {
    val f = new Field(0, 21, 21, cfg, new SeededRng(2))
    val (cx, cy) = (10, 10)
    assert(f.reviveAt(cx, cy, Balanced, 300))
    for d <- 1 to 8 do
      val (dx, dy) = Direction.offset(d)
      assert(f.reviveAt(cx + dx, cy + dy, Balanced, 300))
    assertEquals(census(f), (9, 0))

    for _ <- 0 until 30 do f.tickFast()
    val (live, dead) = census(f)
    // Родню никто не ест: трупов нет, семья на месте (могла подрасти наружу).
    assertEquals(dead, 0, "не должно появиться трупов от каннибализма")
    assert(live >= 9, s"родня не должна гибнуть от своих, live=$live")
  }

  // ── 3. В окружении чужаков (не родня) ─────────────────────
  test("Окружение чужаков: падальщик не нападает на живых (ест только падаль)") {
    val f = new Field(0, 21, 21, cfg, new SeededRng(3))
    val (cx, cy) = (10, 10)
    // Пассивный чужак: 's' без цифры — холостой прыжок, не атакует и не родня.
    val stranger = "ssssssssssssss"
    assert(f.reviveAt(cx, cy, Balanced, 300))
    for d <- 1 to 8 do
      val (dx, dy) = Direction.offset(d)
      assert(f.reviveAt(cx + dx, cy + dy, stranger, 300))
    assertEquals(census(f), (9, 0))

    for _ <- 0 until 30 do f.tickFast()
    val (live, dead) = census(f)
    // 'c' не нацеливается на живых, 'a' в геноме нет — чужаки целы.
    assertEquals(dead, 0, "падальщик не делает трупов из живых чужаков")
    assertEquals(live, 9, "никого не съели живьём")
  }

  // ── 4. В окружении трупов (регрессия «умирает после первого») ──
  test("Окружение трупов: съедает падаль и НЕ умирает после первого трупа") {
    val f = new Field(0, 21, 21, cfg, new SeededRng(4))
    val (cx, cy) = (10, 10)
    assert(f.reviveAt(cx, cy, Balanced, 300))
    // 8 трупов, умерших «от старости» (lifeLeft <= 0) — как в реальной симуляции.
    for d <- 1 to 8 do
      val (dx, dy) = Direction.offset(d)
      assert(f.placeCorpseAt(cx + dx, cy + dy, 0, oldAge = true))
    assertEquals(census(f), (1, 8))

    for _ <- 0 until 40 do f.tickFast()
    val (live, _) = census(f)
    assert(
      live >= 1,
      s"падальщик не должен умирать, поев труп умершего от старости, live=$live"
    )
  }

  test("Окружение трупов: падаль действительно поедается (трупов становится меньше)") {
    val f = new Field(0, 21, 21, cfg, new SeededRng(5))
    val (cx, cy) = (10, 10)
    assert(f.reviveAt(cx, cy, Scavenger, 300))
    for d <- 1 to 8 do
      val (dx, dy) = Direction.offset(d)
      assert(f.placeCorpseAt(cx + dx, cy + dy, 0, oldAge = true))
    val deadBefore = census(f)._2
    assertEquals(deadBefore, 8)

    for _ <- 0 until 40 do f.tickFast()
    val (live, dead) = census(f)
    assert(live >= 1, s"падальщик должен выжить, live=$live")
    assert(dead < deadBefore, s"часть трупов должна быть съедена, осталось $dead из $deadBefore")
  }
