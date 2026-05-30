package com.company.ui

import scala.swing.{Action, BoxPanel, Button, Dimension, FlowPanel, Frame, Label, Orientation, ScrollPane, Separator, TextField}
import scala.swing.event.EditDone
import java.awt.{Color, Font}
import com.company.app.SettingsStore
import com.company.domain.SimulationConfig

/**
 * Окно настроек параметров симуляции. Каждый элемент читает и пишет одно поле
 * иммутабельной [[SimulationConfig]] через [[SettingsStore]] — «настройка»
 * становится атомарной заменой значения, а не правкой глобального `var`.
 * Изменения применяются движком на следующем тике (живая настройка).
 */
class SettingsWindow(store: SettingsStore, onApplySeed: () => Unit) extends Frame:
  title = "Настройки симуляции"

  // Список «обновить отображение» — чтобы кнопка «По умолчанию» перерисовала всё.
  private var refreshers: List[() => Unit] = Nil

  private def clamp(v: Int, min: Int, max: Int): Int =
    math.max(min, math.min(max, v))

  /**
   * Блок «подпись − [значение] +» для регулировки одного целого параметра.
   * get читает поле конфигурации, set возвращает изменённую копию конфигурации.
   */
  private def control(
      name: String,
      get: SimulationConfig => Int,
      set: (SimulationConfig, Int) => SimulationConfig,
      step: Int = 1,
      min: Int = 0,
      max: Int = Int.MaxValue
  ): FlowPanel =
    def read(): Int = get(store.current)
    def write(n: Int): Unit = store.update(c => set(c, clamp(n, min, max)))

    val value = new TextField(read().toString)
    value.font = new Font("Monospaced", Font.BOLD, 13)
    value.preferredSize = new Dimension(70, value.preferredSize.height)
    value.horizontalAlignment = scala.swing.Alignment.Right
    def refresh(): Unit = value.text = read().toString
    refreshers ::= (() => refresh())
    value.listenTo(value)
    value.reactions += { case EditDone(_) =>
      value.text.trim.toIntOption match
        case Some(n) => write(n); refresh()
        case None    => refresh()
    }
    val minus = new Button(Action("\u2212") { write(read() - step); refresh() })
    val plus = new Button(Action("+") { write(read() + step); refresh() })
    val caption = new Label(s"$name:")
    caption.font = new Font("SansSerif", Font.PLAIN, 13)
    caption.preferredSize = new Dimension(220, caption.preferredSize.height)
    caption.horizontalAlignment = scala.swing.Alignment.Left
    for b <- Seq(minus, plus) do b.font = new Font("SansSerif", Font.BOLD, 14)
    new FlowPanel(FlowPanel.Alignment.Left)(caption, minus, value, plus)

  private def section(text: String): Label =
    val l = new Label(text)
    l.font = new Font("SansSerif", Font.BOLD, 15)
    l.foreground = new Color(40, 70, 120)
    l.horizontalAlignment = scala.swing.Alignment.Left
    l

  private val content = new BoxPanel(Orientation.Vertical) {
    contents += section("Воспроизводимость")
    contents += control("Seed",
      _.seed.toInt, (c, v) => c.copy(seed = v.toLong), step = 1, min = Int.MinValue)
    contents += new FlowPanel(FlowPanel.Alignment.Left)(
      new Button(Action("Применить seed (сброс)") { onApplySeed() })
    )

    contents += new Separator
    contents += section("Сезоны")
    contents += control("Длительность лета (тиков)",
      _.summerTicks, (c, v) => c.copy(summerTicks = v), step = 10, min = 1)
    contents += control("Длительность зимы (тиков)",
      _.winterTicks, (c, v) => c.copy(winterTicks = v), step = 10, min = 1)

    contents += new Separator
    contents += section("Гравитация")
    contents += control("Смещение на 1 вниз (тиков, 0=выкл)",
      _.gravityTicks, (c, v) => c.copy(gravityTicks = v), step = 50, min = 0)

    contents += new Separator
    contents += section("Освещённость (фотосинтез)")
    contents += control("Макс. солнце летом",
      _.summerLight, (c, v) => c.copy(summerLight = v), step = 5, min = 0)
    contents += control("Макс. солнце зимой",
      _.winterLight, (c, v) => c.copy(winterLight = v), step = 5, min = 0)

    contents += new Separator
    contents += section("Энергия")
    contents += control("Стартовая энергия",
      _.startEnergy, (c, v) => c.copy(startEnergy = v), step = 10, min = 1)
    contents += control("Стоимость деления",
      _.energyForDel, (c, v) => c.copy(energyForDel = v), step = 10, min = 0)
    contents += control("Стоимость шага/атаки",
      _.energyForStep, (c, v) => c.copy(energyForStep = v), step = 10, min = 0)
    contents += control("Порог принуд. деления",
      _.reproduceThreshold, (c, v) => c.copy(reproduceThreshold = v), step = 100, min = 1)
    contents += control("Стоимость принуд. деления",
      _.forcedReproduceCost, (c, v) => c.copy(forcedReproduceCost = v), step = 100, min = 0)

    contents += new Separator
    contents += section("Хищничество")
    contents += control("Бонус за труп (падальщик)",
      _.corpseBonus, (c, v) => c.copy(corpseBonus = v), step = 100, min = 0)
    contents += control("Стоимость падальщика (c)",
      _.scavengeCost, (c, v) => c.copy(scavengeCost = v), step = 10, min = 0)
    contents += control("Исчезн. трупов (тиков, -1=нет)",
      _.corpseDecayTicks, (c, v) => c.copy(corpseDecayTicks = v), step = 10, min = -1)
    contents += control("Бонус за добычу (хищник)",
      _.weakPreyBonus, (c, v) => c.copy(weakPreyBonus = v), step = 100, min = 0)
    contents += control("Штраф за атаку",
      _.retribution, (c, v) => c.copy(retribution = v), step = 5, min = 0)
    contents += control("Преимущество добычи",
      _.preyAdvantage, (c, v) => c.copy(preyAdvantage = v), step = 5, min = 0)

    contents += new Separator
    contents += section("Генетика")
    contents += control("Порог родства (%)",
      _.kinshipThresholdPct, (c, v) => c.copy(kinshipThresholdPct = v), step = 1, min = 0, max = 100)
    contents += control("Макс. длина генома",
      _.maxGenLength, (c, v) => c.copy(maxGenLength = v), step = 5, min = 1)
    contents += control("Шанс мутации 1/N",
      _.mutationChance, (c, v) => c.copy(mutationChance = v), step = 1, min = 1)
    contents += control("Шанс вставки гена 1/N",
      _.insertChance, (c, v) => c.copy(insertChance = v), step = 1, min = 1)

    contents += new Separator
    contents += new FlowPanel(FlowPanel.Alignment.Left)(
      new Button(Action("Вернуть по умолчанию") {
        store.reset()
        refreshers.foreach(_())
      })
    )
  }

  contents = new ScrollPane(content)
  size = new Dimension(440, 640)
