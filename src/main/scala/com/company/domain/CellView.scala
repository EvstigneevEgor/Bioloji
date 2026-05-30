package com.company.domain

/**
 * Read-only проекция клетки для слоя отображения. UI зависит только от этого
 * трейта (а не от мутабельной [[Cell]]), поэтому не может менять состояние
 * домена — только читать. [[Cell]] реализует трейт напрямую, без аллокаций
 * на кадр отрисовки.
 */
trait CellView:
  def isAlive: Boolean
  def isCorpse: Boolean
  /** Умерла именно в этот ход (для тёмной вспышки на один кадр). */
  def diedThisTick: Boolean
  /** Родилась именно в этот ход (для подсветки/счётчика рождений). */
  def bornThisTick: Boolean
  def energy: Int
  def lifeLeft: Int
  def genome: Genome
  def pointer: Int
  def diet: Diet
  /** Идентификатор клетки (0 — нет клетки). */
  def id: Long
