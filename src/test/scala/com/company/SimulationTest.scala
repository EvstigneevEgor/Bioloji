package com.company

class SimulationTest extends munit.FunSuite:

  test("Rng: одинаковый seed даёт одинаковую последовательность") {
    Rng.seed(123)
    val a = List.fill(10)(Rng.int(1000))
    Rng.seed(123)
    val b = List.fill(10)(Rng.int(1000))
    assertEquals(a, b)
  }

  test("Kletka: reviv делает клетку живой, без трупа/смерти") {
    val k = new Kletka
    k.reviv("ff")
    assert(k.isLive)
    assert(!k.isCorpse)
    assert(!k.isDead)
    assertEquals(k.gen, "ff")
  }

  test("Kletka: жизненный цикл состояний (живая -> черный -> серый труп -> пусто)") {
    val k = new Kletka
    k.reviv("ff")
    assert(k.isLive)

    k.dead()
    assert(!k.isLive)
    assert(k.isCorpse)
    assert(k.isDead) // тик смерти отрисовывается чёрным

    k.itnew()
    assert(k.isCorpse) // далее — серый труп
    assert(!k.isDead)

    k.del()
    assert(!k.isLive)
    assert(!k.isCorpse)
    assert(!k.isDead)
  }

  test("Kletka.isParents: идентичные геномы — родня") {
    val a = new Kletka; a.creategen("ffffssss")
    val b = new Kletka; b.creategen("ffffssss")
    assert(a.isParents(b))
  }

  test("Kletka.isParents: полностью разные геномы — не родня") {
    val a = new Kletka; a.creategen("ffffffffff")
    val b = new Kletka; b.creategen("ssssssssss")
    assert(!a.isParents(b))
  }

  test("Kletka.isParents: пустой геном — не родня") {
    val a = new Kletka
    val b = new Kletka; b.creategen("ffff")
    assert(!a.isParents(b))
  }

  test("Pole: некорректное n отбрасывается require") {
    intercept[IllegalArgumentException] {
      new Pole(99, 50, 50)
    }
  }

  test("Pole: симуляция детерминирована при фиксированном seed") {
    def runAndCount(): Int =
      Rng.seed(42)
      val p = new Pole(4, 60, 60)
      for _ <- 0 until 50 do p.itr()
      (for i <- 0 until p.W; j <- 0 until p.H if p.getLive(i, j) yield 1).sum

    val first = runAndCount()
    val second = runAndCount()
    assertEquals(first, second)
  }

  test("Pole: множество шагов не бросает исключений") {
    Rng.seed(7)
    val p = new Pole(4, 80, 80)
    for _ <- 0 until 200 do p.itr()
  }
