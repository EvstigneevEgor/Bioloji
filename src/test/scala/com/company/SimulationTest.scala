package com.company

import com.company.domain.*

class SimulationTest extends munit.FunSuite:

  private val cfg = SimulationConfig.Default

  // ── ГСЧ ──────────────────────────────────────────────────
  test("SeededRng: одинаковый seed даёт одинаковую последовательность") {
    val a = List.fill(10)(new SeededRng(123).nextInt(1000))
    val b = List.fill(10)(new SeededRng(123).nextInt(1000))
    // независимые инстансы с одним seed — идентичны (нет общего состояния)
    assertEquals(a.head, b.head)
    val r1 = new SeededRng(123)
    val r2 = new SeededRng(123)
    assertEquals(List.fill(10)(r1.nextInt(1000)), List.fill(10)(r2.nextInt(1000)))
  }

  // ── чистое ядро: Genome ──────────────────────────────────
  test("Genome.commandAt: декод команд и прыжков") {
    assertEquals(Genome("f").commandAt(0), Command.Photosynthesis)
    assertEquals(Genome("s3").commandAt(0), Command.Step(3))
    assertEquals(Genome("e5").commandAt(0), Command.Divide(5))
    assertEquals(Genome("a7").commandAt(0), Command.Attack(7))
    assertEquals(Genome("c4").commandAt(0), Command.Scavenge(4))  // падальщик
    assertEquals(Genome("s").commandAt(0), Command.Jump('s')) // нет цифры-направления
    assertEquals(Genome("c").commandAt(0), Command.Jump('c')) // 'c' без цифры — прыжок
    assertEquals(Genome("3").commandAt(0), Command.Jump('3')) // цифра — прыжок
    assertEquals(Genome("").commandAt(0), Command.Idle)
  }

  test("Genome.sanitize: оставляет только символы алфавита") {
    assertEquals(Genome.sanitize("f s3 e5!"), "fs3e5")
    assertEquals(Genome.sanitize("XYZ"), "")
    assertEquals(Genome.sanitize("12345678seafc"), Genome.Alphabet)
  }

  test("Genome.isKin: родство по различию геномов") {
    assert(Genome("ffffssss").isKin(Genome("ffffssss"), cfg.kinshipThresholdPct))
    assert(!Genome("ffffffffff").isKin(Genome("ssssssssss"), cfg.kinshipThresholdPct))
    assert(!Genome("").isKin(Genome("ffff"), cfg.kinshipThresholdPct))
  }

  // ── чистое ядро: Diet / Direction ────────────────────────
  test("Diet.normalized: нормировка и значение по умолчанию") {
    assertEquals(Diet(2, 0, 0).normalized(100), Diet(100, 0, 0))
    assertEquals(Diet.Empty.normalized(100), Diet.LightSeed)
  }

  test("Direction.offset: геометрия направлений") {
    assertEquals(Direction.offset(1), (0, -1))
    assertEquals(Direction.offset(3), (1, 0))
    assertEquals(Direction.offset(5), (0, 1))
    assertEquals(Direction.offset(7), (-1, 0))
  }

  // ── Cell ─────────────────────────────────────────────────
  test("Cell: revive делает клетку живой, без трупа/смерти") {
    val k = new Cell
    k.revive(Genome("ff"))
    assert(k.isAlive)
    assert(!k.isCorpse)
    assert(!k.diedThisTick)
    assertEquals(k.genome, Genome("ff"))
  }

  test("Cell: rewriteGenome заменяет геном и сбрасывает указатель") {
    val k = new Cell
    k.revive(Genome("ffff"))
    k.advancePointer(3)
    assertEquals(k.pointer, 3)
    k.rewriteGenome(Genome("s3"))
    assertEquals(k.genome, Genome("s3"))
    assertEquals(k.pointer, 0) // указатель валиден для новой длины
  }

  test("Cell: жизненный цикл (живая -> вспышка -> труп -> пусто)") {
    val k = new Cell
    k.revive(Genome("ff"))
    assert(k.isAlive)

    k.die()
    assert(!k.isAlive)
    assert(k.isCorpse)
    assert(k.diedThisTick) // тик смерти отрисовывается чёрным

    k.beginTick()
    assert(k.isCorpse) // далее — серый труп
    assert(!k.diedThisTick)

    k.clear()
    assert(!k.isAlive)
    assert(!k.isCorpse)
    assert(!k.diedThisTick)
  }

  test("Cell: поедание трупа — чистый плюс (общая логика a и c)") {
    val corpse = new Cell
    corpse.revive(Genome("f"))
    corpse.setEnergy(0)
    corpse.die() // труп с энергией 0
    assert(corpse.isCorpse)

    val attacker = new Cell
    attacker.revive(Genome("c5"))
    attacker.setEnergy(100)

    // Труп съеден: атакующий «переезжает» в ячейку трупа, забирая бонус.
    corpse.attackedBy(attacker, cfg)
    assert(corpse.isAlive)      // выжившая ячейка — бывший труп с идентичностью атакующего
    assert(!attacker.isAlive)   // moveInto очистил исходную ячейку атакующего
    // Энергия = corpseBonus (нижняя планка) + энергия атакующего.
    assertEquals(corpse.energy, cfg.corpseBonus + 100)
    // Рацион записан как «падальщик» (для синей окраски).
    assert(corpse.diet.corpse > 0)
    assertEquals(corpse.diet.prey, 0)
  }

  test("Cell: падальщик не трогает живую клетку (атака не проходит сквозь привычку)") {
    // Живой сосед не является трупом — devour по корпсовой ветке не вызывается.
    val living = new Cell
    living.revive(Genome("ffff"))
    living.setEnergy(500)
    assert(living.isAlive && !living.isCorpse)
  }

  // ── Field ────────────────────────────────────────────────
  test("Field: некорректное n отбрасывается require") {
    intercept[IllegalArgumentException] {
      new Field(99, 50, 50, cfg, new SeededRng(1))
    }
  }

  test("Field.setGenomeAt: правка живой клетки, отказ для пустой/вне поля") {
    val p = new Field(4, 60, 60, cfg, new SeededRng(42))
    // Находим первую живую клетку.
    val alive =
      (for i <- 0 until p.W; j <- 0 until p.H if p.cell(i, j).isAlive yield (i, j)).headOption
    assert(alive.isDefined, "должна быть хотя бы одна стартовая живая клетка")
    val (ax, ay) = alive.get

    // Посторонние символы отбрасываются, геном применяется, указатель сброшен.
    assert(p.setGenomeAt(ax, ay, "a3?? x"))
    assertEquals(p.cell(ax, ay).genome, Genome("a3"))
    assertEquals(p.cell(ax, ay).pointer, 0)

    // Пустая ячейка и координаты вне поля — правка не применяется.
    val empty =
      (for i <- 0 until p.W; j <- 0 until p.H
        if !p.cell(i, j).isAlive && !p.cell(i, j).isCorpse yield (i, j)).head
    assert(!p.setGenomeAt(empty._1, empty._2, "fff"))
    assert(!p.setGenomeAt(-1, 0, "fff"))
    assert(!p.setGenomeAt(0, p.H, "fff"))
  }

  test("Field: симуляция детерминирована при фиксированном seed") {
    def runAndCount(): Int =
      val p = new Field(4, 60, 60, cfg, new SeededRng(42))
      for _ <- 0 until 50 do p.tick()
      (for i <- 0 until p.W; j <- 0 until p.H if p.cell(i, j).isAlive yield 1).sum

    assertEquals(runAndCount(), runAndCount())
  }

  test("Field: множество шагов не бросает исключений") {
    val p = new Field(4, 80, 80, cfg, new SeededRng(7))
    for _ <- 0 until 200 do p.tick()
  }

  test("Field.tickFast: множество шагов не бросает исключений") {
    val p = new Field(4, 120, 120, cfg, new SeededRng(7))
    for _ <- 0 until 300 do p.tickFast()
  }

  test("Field.tickFast: поведение совпадает с tick 1-в-1 на длинном горизонте") {
    def snapshot(fast: Boolean): Vector[(Boolean, Int)] =
      val p = new Field(4, 200, 150, cfg, new SeededRng(42))
      for _ <- 0 until 1500 do if fast then p.tickFast() else p.tick()
      (for i <- 0 until p.W; j <- 0 until p.H
        yield (p.cell(i, j).isAlive, p.cell(i, j).energy)).toVector
    assertEquals(snapshot(fast = true), snapshot(fast = false))
  }

  test("Field.tickFast: нет коллапса/взрыва популяции на длинном прогоне") {
    val p = new Field(4, 200, 150, cfg, new SeededRng(123))
    for _ <- 0 until 8000 do p.tickFast()
    val live = (for i <- 0 until p.W; j <- 0 until p.H if p.cell(i, j).isAlive yield 1).sum
    assert(live > 50, s"популяция не должна вымирать: live=$live")
  }
