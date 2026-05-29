package com.company

import scala.swing.{MainFrame, Dimension}
import javax.swing.Timer
import java.awt.event.{ActionEvent, ActionListener}

object Window:
  /** Интервал шага симуляции, мс (FIX: явная именованная константа). */
  private val TickIntervalMs = 60
  /** Каждые сколько тиков меняется сезон. */
  private val TicksPerSeason = 200

/** Главное окно. Симуляцию двигает javax.swing.Timer (в EDT), не paint. */
class Window extends MainFrame:
  import Window.*

  title = "Bioloji"
  resizable = false

  private val petri = new Petri
  contents = petri

  size = new Dimension(petri.widthWin + 30, petri.heightWin + 70)

  private var tickCount = 0

  private val timer = new Timer(
    TickIntervalMs,
    new ActionListener:
      def actionPerformed(e: ActionEvent): Unit =
        petri.pole.itr()
        tickCount += 1
        if tickCount % TicksPerSeason == 0 then
          petri.pole.year()
        petri.repaint()
  )
  timer.start()
