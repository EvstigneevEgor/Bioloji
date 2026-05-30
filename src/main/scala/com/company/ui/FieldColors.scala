package com.company.ui

import java.awt.Color
import com.company.domain.Diet

/**
 * Чистая логика окраски поля — отделена от отрисовки и от домена. Кодирует
 * состояние клетки цветом: оттенок = рацион (чем питается), яркость = энергия;
 * фон = освещённость среды.
 */
object FieldColors:
  /** Цвет трупа (мёртвая органика — пища падальщиков). */
  val Corpse = new Color(120, 112, 104)
  /** Цвет «только что умершей» клетки (вспышка на один кадр). */
  val Dead = new Color(25, 25, 25)

  // Базовые цвета рациона: смешиваются пропорционально съеденному.
  private val DietPrey = (225.0, 60.0, 60.0)    // хищник     -> красный
  private val DietLight = (70.0, 205.0, 70.0)   // фотосинтез -> зелёный
  private val DietCorpse = (80.0, 120.0, 245.0) // падальщик  -> синий

  // Цвета фона-освещённости: тень/ночь (свет=0) -> солнце (свет=макс).
  private val ShadeColor = new Color(24, 26, 38)
  private val SunColor = new Color(72, 68, 48)

  /** Линейная интерполяция между двумя цветами. */
  def lerp(a: Color, b: Color, t: Double): Color =
    val tt = math.max(0.0, math.min(1.0, t))
    new Color(
      (a.getRed + (b.getRed - a.getRed) * tt).toInt,
      (a.getGreen + (b.getGreen - a.getGreen) * tt).toInt,
      (a.getBlue + (b.getBlue - a.getBlue) * tt).toInt
    )

  /** Цвет среды по освещённости: тень снизу -> солнце сверху. */
  def light(value: Int, maxLight: Int): Color =
    lerp(ShadeColor, SunColor, value.toDouble / maxLight)

  /** Цвет клетки: оттенок = чем питается, яркость = сколько энергии. */
  def cell(energy: Int, diet: Diet): Color =
    val total = diet.total
    val (br, bg, bb) =
      if total <= 0 then (190.0, 190.0, 190.0) // ещё не питалась
      else
        val wp = diet.prey.toDouble / total
        val wl = diet.light.toDouble / total
        val wc = diet.corpse.toDouble / total
        (
          wp * DietPrey._1 + wl * DietLight._1 + wc * DietCorpse._1,
          wp * DietPrey._2 + wl * DietLight._2 + wc * DietCorpse._2,
          wp * DietPrey._3 + wl * DietLight._3 + wc * DietCorpse._3
        )
    // Яркость по энергии: голодная — тёмная, сытая — яркая (плавное насыщение).
    val frac = math.max(0, energy).toDouble / (math.max(0, energy) + 1500.0)
    val bright = 0.35 + 0.65 * frac
    new Color(
      math.min(255, (br * bright).toInt),
      math.min(255, (bg * bright).toInt),
      math.min(255, (bb * bright).toInt)
    )
