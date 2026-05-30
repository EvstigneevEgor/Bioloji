package com.company

import scala.swing.{Action, BoxPanel, Button, Dimension, FlowPanel, Frame, Label, Orientation, ScrollPane, Separator, TextField}
import scala.swing.event.EditDone
import java.awt.{Color, Font}

/**
 * Отдельное окно настроек всех «захардкоженных» параметров симуляции:
 * длительность сезонов, максимальное солнце, стоимости/награды энергии,
 * параметры хищничества и генетики.
 *
 * Все элементы читают и пишут изменяемые переменные напрямую в объектах
 * [[Pole]] и [[Kletka]], поэтому изменения сразу влияют на симуляцию
 * (без перезапуска). Значения-длительности и солнце хранятся в companion-
 * объектах, так что переживают сброс симуляции (кнопка «стоп»).
 */
class SettingsWindow(onApplySeed: () => Unit) extends Frame:
  import SettingsWindow.*

  title = "Настройки симуляции"

  // Список «обновить отображение» — чтобы кнопка «По умолчанию» перерисовала всё.
  private var refreshers: List[() => Unit] = Nil

  /** Ограничить значение в допустимых границах [min, max]. */
  private def clamp(v: Int, min: Int, max: Int): Int =
    math.max(min, math.min(max, v))

  /**
   * Блок «подпись − [значение] +» для регулировки одного целого параметра.
   * Значение можно править кнопками −/+ или ввести вручную в поле и нажать
   * Enter (либо увести фокус). get/set читают и записывают переменную модели;
   * step — шаг изменения, min/max — границы.
   */
  private def control(
      name: String,
      get: () => Int,
      set: Int => Unit,
      step: Int = 1,
      min: Int = 0,
      max: Int = Int.MaxValue
  ): FlowPanel =
    val value = new TextField(get().toString)
    value.font = new Font("Monospaced", Font.BOLD, 13)
    value.preferredSize = new Dimension(70, value.preferredSize.height)
    value.horizontalAlignment = scala.swing.Alignment.Right
    def refresh(): Unit = value.text = get().toString
    refreshers ::= (() => refresh())
    // Ручной ввод: распарсить число, ограничить границами, применить.
    // При некорректном вводе — вернуть прежнее значение.
    value.listenTo(value)
    value.reactions += { case EditDone(_) =>
      value.text.trim.toIntOption match
        case Some(n) => set(clamp(n, min, max)); refresh()
        case None    => refresh()
    }
    val minus = new Button(Action("\u2212") {
      set(clamp(get() - step, min, max)); refresh()
    })
    val plus = new Button(Action("+") {
      set(clamp(get() + step, min, max)); refresh()
    })
    val caption = new Label(s"$name:")
    caption.font = new Font("SansSerif", Font.PLAIN, 13)
    caption.preferredSize = new Dimension(220, caption.preferredSize.height)
    caption.horizontalAlignment = scala.swing.Alignment.Left
    for b <- Seq(minus, plus) do b.font = new Font("SansSerif", Font.BOLD, 14)
    new FlowPanel(FlowPanel.Alignment.Left)(caption, minus, value, plus)

  /** Заголовок секции настроек. */
  private def section(text: String): Label =
    val l = new Label(text)
    l.font = new Font("SansSerif", Font.BOLD, 15)
    l.foreground = new Color(40, 70, 120)
    l.horizontalAlignment = scala.swing.Alignment.Left
    l

  private val content = new BoxPanel(Orientation.Vertical) {
    contents += section("Воспроизводимость")
    contents += control("Seed",
      () => Pole.Seed.toInt, v => Pole.Seed = v.toLong, step = 1, min = Int.MinValue)
    contents += new FlowPanel(FlowPanel.Alignment.Left)(
      new Button(Action("Применить seed (сброс)") {
        Rng.seed(Pole.Seed)
        onApplySeed()
      })
    )

    contents += new Separator
    contents += section("Сезоны")
    contents += control("Длительность лета (тиков)",
      () => Pole.SummerTicks, v => Pole.SummerTicks = v, step = 10, min = 1)
    contents += control("Длительность зимы (тиков)",
      () => Pole.WinterTicks, v => Pole.WinterTicks = v, step = 10, min = 1)

    contents += new Separator
    contents += section("Гравитация")
    contents += control("Смещение на 1 вниз (тиков, 0=выкл)",
      () => Pole.GravityTicks, v => Pole.GravityTicks = v, step = 50, min = 0)

    contents += new Separator
    contents += section("Освещённость (фотосинтез)")
    contents += control("Макс. солнце летом",
      () => Pole.SummerLight, v => Pole.SummerLight = v, step = 5, min = 0)
    contents += control("Макс. солнце зимой",
      () => Pole.WinterLight, v => Pole.WinterLight = v, step = 5, min = 0)

    contents += new Separator
    contents += section("Энергия")
    contents += control("Стартовая энергия",
      () => Pole.StartEnergy, v => Pole.StartEnergy = v, step = 10, min = 1)
    contents += control("Стоимость деления",
      () => Pole.EnergyForDel, v => Pole.EnergyForDel = v, step = 10, min = 0)
    contents += control("Стоимость шага/атаки",
      () => Pole.EnergyForStep, v => Pole.EnergyForStep = v, step = 10, min = 0)
    contents += control("Порог принуд. деления",
      () => Pole.ReproduceThreshold, v => Pole.ReproduceThreshold = v, step = 100, min = 1)
    contents += control("Стоимость принуд. деления",
      () => Pole.ForcedReproduceCost, v => Pole.ForcedReproduceCost = v, step = 100, min = 0)

    contents += new Separator
    contents += section("Хищничество")
    contents += control("Бонус за труп (падальщик)",
      () => Kletka.CorpseBonus, v => Kletka.CorpseBonus = v, step = 100, min = 0)
    contents += control("Исчезн. трупов (тиков, -1=нет)",
      () => Kletka.CorpseDecayTicks, v => Kletka.CorpseDecayTicks = v, step = 10, min = -1)
    contents += control("Бонус за добычу (хищник)",
      () => Kletka.WeakPreyBonus, v => Kletka.WeakPreyBonus = v, step = 100, min = 0)
    contents += control("Штраф за атаку",
      () => Kletka.Retribution, v => Kletka.Retribution = v, step = 5, min = 0)
    contents += control("Преимущество добычи",
      () => Kletka.PreyAdvantage, v => Kletka.PreyAdvantage = v, step = 5, min = 0)

    contents += new Separator
    contents += section("Генетика")
    contents += control("Порог родства (%)",
      () => Kletka.KinshipThresholdPct, v => Kletka.KinshipThresholdPct = v, step = 1, min = 0, max = 100)
    contents += control("Макс. длина генома",
      () => Kletka.MaxGenLength, v => Kletka.MaxGenLength = v, step = 5, min = 1)
    contents += control("Шанс мутации 1/N",
      () => Kletka.MutationChance, v => Kletka.MutationChance = v, step = 1, min = 1)
    contents += control("Шанс вставки гена 1/N",
      () => Kletka.InsertChance, v => Kletka.InsertChance = v, step = 1, min = 1)

    contents += new Separator
    contents += new FlowPanel(FlowPanel.Alignment.Left)(
      new Button(Action("Вернуть по умолчанию") {
        restoreDefaults()
        refreshers.foreach(_())
      })
    )
  }

  contents = new ScrollPane(content)
  size = new Dimension(440, 640)

object SettingsWindow:
  /** Сбрасывает все настраиваемые параметры к исходным значениям. */
  def restoreDefaults(): Unit =
    Pole.Seed = 42
    Pole.SummerTicks = 200
    Pole.WinterTicks = 200
    Pole.GravityTicks = 400
    Pole.SummerLight = 100
    Pole.WinterLight = 25
    Pole.StartEnergy = 150
    Pole.EnergyForDel = 150
    Pole.EnergyForStep = 250
    Pole.ReproduceThreshold = 150 * 50
    Pole.ForcedReproduceCost = 150 * 40
    Kletka.CorpseBonus = 800
    Kletka.CorpseDecayTicks = -1
    Kletka.WeakPreyBonus = 400
    Kletka.Retribution = 20
    Kletka.PreyAdvantage = 10
    Kletka.KinshipThresholdPct = 20
    Kletka.MaxGenLength = 100
    Kletka.MutationChance = 6
    Kletka.InsertChance = 5
