import { Injectable } from '@angular/core'

export enum ProgressionEase {
  LINEAR,
  EASE_IN_SINE,
  EASE_OUT_SINE,
  EASE_IN_OUT_SINE,
  EASE_IN_QUAD,
  EASE_OUT_QUAD,
  EASE_IN_OUT_QUAD,
  EASE_IN_CUBIC,
  EASE_OUT_CUBIC,
  EASE_IN_OUT_CUBIC,
  EASE_IN_QUART,
  EASE_OUT_QUART,
  EASE_IN_OUT_QUART,
  EASE_IN_QUINT,
  EASE_OUT_QUINT,
  EASE_IN_OUT_QUINT,
  EASE_IN_EXPO,
  EASE_OUT_EXPO,
  EASE_IN_OUT_EXPO,
  EASE_IN_CIRC,
  EASE_OUT_CIRC,
  EASE_IN_OUT_CIRC,
  EASE_IN_BACK,
  EASE_OUT_BACK,
  EASE_IN_OUT_BACK,
  EASE_IN_ELASTIC,
  EASE_OUT_ELASTIC,
  EASE_IN_OUT_ELASTIC,
  EASE_IN_BOUNCE,
  EASE_OUT_BOUNCE,
  EASE_IN_OUT_BOUNCE,
}

/** @url <https://nicmulvaney.com/easing> */
@Injectable({ providedIn: 'root' })
export class Easing {
  public ease(x: number, ease: ProgressionEase = ProgressionEase.LINEAR): number {
    switch (ease) { // NOSONAR
      case ProgressionEase.LINEAR: return this.easeLinear(x)
      case ProgressionEase.EASE_IN_SINE: return this.easeInSine(x)
      case ProgressionEase.EASE_OUT_SINE: return this.easeOutSine(x)
      case ProgressionEase.EASE_IN_OUT_SINE: return this.easeInOutSine(x)
      case ProgressionEase.EASE_IN_QUAD: return this.easeInQuad(x)
      case ProgressionEase.EASE_OUT_QUAD: return this.easeOutQuad(x)
      case ProgressionEase.EASE_IN_OUT_QUAD: return this.easeInOutQuad(x)
      case ProgressionEase.EASE_IN_CUBIC: return this.easeInCubic(x)
      case ProgressionEase.EASE_OUT_CUBIC: return this.easeOutCubic(x)
      case ProgressionEase.EASE_IN_OUT_CUBIC: return this.easeInOutCubic(x)
      case ProgressionEase.EASE_IN_QUART: return this.easeInQuart(x)
      case ProgressionEase.EASE_OUT_QUART: return this.easeOutQuart(x)
      case ProgressionEase.EASE_IN_OUT_QUART: return this.easeInOutQuart(x)
      case ProgressionEase.EASE_IN_QUINT: return this.easeInQuint(x)
      case ProgressionEase.EASE_OUT_QUINT: return this.easeOutQuint(x)
      case ProgressionEase.EASE_IN_OUT_QUINT: return this.easeInOutQuint(x)
      case ProgressionEase.EASE_IN_EXPO: return this.easeInExpo(x)
      case ProgressionEase.EASE_OUT_EXPO: return this.easeOutExpo(x)
      case ProgressionEase.EASE_IN_OUT_EXPO: return this.easeInOutExpo(x)
      case ProgressionEase.EASE_IN_CIRC: return this.easeInCirc(x)
      case ProgressionEase.EASE_OUT_CIRC: return this.easeOutCirc(x)
      case ProgressionEase.EASE_IN_OUT_CIRC: return this.easeInOutCirc(x)
      case ProgressionEase.EASE_IN_BACK: return this.easeInBack(x)
      case ProgressionEase.EASE_OUT_BACK: return this.easeOutBack(x)
      case ProgressionEase.EASE_IN_OUT_BACK: return this.easeInOutBack(x)
      case ProgressionEase.EASE_IN_ELASTIC: return this.easeInElastic(x)
      case ProgressionEase.EASE_OUT_ELASTIC: return this.easeOutElastic(x)
      case ProgressionEase.EASE_IN_OUT_ELASTIC: return this.easeInOutElastic(x)
      case ProgressionEase.EASE_IN_BOUNCE: return this.easeInBounce(x)
      case ProgressionEase.EASE_OUT_BOUNCE: return this.easeOutBounce(x)
      case ProgressionEase.EASE_IN_OUT_BOUNCE: return this.easeInOutBounce(x)
      default: throw new Error(`Unknown ease ${ease}`)
    }
  }

  private readonly easeLinear = (x: number) => x
  private readonly easeInSine = (x: number) => 1 - Math.cos((x * Math.PI) / 2)
  private readonly easeOutSine = (x: number) => Math.sin((x * Math.PI) / 2)
  private readonly easeInOutSine = (x: number) => -(Math.cos(Math.PI * x) - 1) / 2

  private readonly easeInQuad = (x: number) => x * x
  private readonly easeOutQuad = (x: number) => 1 - (1 - x) * (1 - x)
  private readonly easeInOutQuad = (x: number) => x < 0.5 ? 2 * x * x : 1 - Math.pow(-2 * x + 2, 2) / 2

  private readonly easeInCubic = (x: number) => x * x * x
  private readonly easeOutCubic = (x: number) => 1 - Math.pow(1 - x, 3)
  private readonly easeInOutCubic = (x: number) => x < 0.5 ? 4 * x * x * x : 1 - Math.pow(-2 * x + 2, 3) / 2

  private readonly easeInQuart = (x: number) => x * x * x * x
  private readonly easeOutQuart = (x: number) => 1 - Math.pow(1 - x, 4)
  private readonly easeInOutQuart = (x: number) => x < 0.5 ? 8 * x * x * x * x : 1 - Math.pow(-2 * x + 2, 4) / 2

  private readonly easeInQuint = (x: number) => x * x * x * x * x
  private readonly easeOutQuint = (x: number) => 1 - Math.pow(1 - x, 5)
  private readonly easeInOutQuint = (x: number) => x < 0.5 ? 16 * x * x * x * x * x : 1 - Math.pow(-2 * x + 2, 5) / 2

  private readonly easeInExpo = (x: number) => x === 0 ? 0 : Math.pow(2, 10 * x - 10)
  private readonly easeOutExpo = (x: number) => x === 1 ? 1 : 1 - Math.pow(2, -10 * x)
  private readonly easeInOutExpo = (x: number) => {
    if (x === 0 || x === 1) {
      return x
    }
    return x < 0.5 ? Math.pow(2, 20 * x - 10) / 2 : (2 - Math.pow(2, -20 * x + 10)) / 2
  }

  private readonly easeInCirc = (x: number) => 1 - Math.sqrt(1 - Math.pow(x, 2))
  private readonly easeOutCirc = (x: number) => Math.sqrt(1 - Math.pow(x - 1, 2))
  private readonly easeInOutCirc = (x: number) => x < 0.5
    ? (1 - Math.sqrt(1 - Math.pow(2 * x, 2))) / 2
    : (Math.sqrt(1 - Math.pow(-2 * x + 2, 2)) + 1) / 2

  private readonly easeInBack = (x: number) => {
    const c1 = 1.70158
    const c3 = c1 + 1
    return c3 * x * x * x - c1 * x * x
  }

  private readonly easeOutBack = (x: number) => {
    const c1 = 1.70158
    const c3 = c1 + 1
    return 1 + c3 * Math.pow(x - 1, 3) + c1 * Math.pow(x - 1, 2)
  }

  private readonly easeInOutBack = (x: number) => {
    const c1 = 1.70158
    const c2 = c1 * 1.525
    return x < 0.5
      ? (Math.pow(2 * x, 2) * ((c2 + 1) * 2 * x - c2)) / 2
      : (Math.pow(2 * x - 2, 2) * ((c2 + 1) * (x * 2 - 2) + c2) + 2) / 2
  }

  private readonly easeOutBounce = (x: number) => {
    const n1 = 7.5625
    const d1 = 2.75
    if (x < 1 / d1) {
      return n1 * x * x
    }
    else if (x < 2 / d1) {
      x -= 1.5
      return n1 * (x / d1) * x + 0.75
    }
    else if (x < 2.5 / d1) {
      x -= 2.25
      return n1 * (x / d1) * x + 0.9375
    }
    else {
      x -= 2.625
      return n1 * (x / d1) * x + 0.984375
    }
  }

  private readonly easeInBounce = (x: number) => 1 - this.easeOutBounce(1 - x)
  private readonly easeInOutBounce = (x: number) => x < 0.5
    ? (1 - this.easeOutBounce(1 - 2 * x)) / 2
    : (1 + this.easeOutBounce(2 * x - 1)) / 2

  private readonly easeInElastic = (x: number) => {
    const c4 = (2 * Math.PI) / 3
    if (x === 0 || x === 1) {
      return x
    }
    return -Math.pow(2, 10 * x - 10) * Math.sin((x * 10 - 10.75) * c4)
  }

  private readonly easeOutElastic = (x: number) => {
    const c4 = (2 * Math.PI) / 3
    if (x === 0 || x === 1) {
      return x
    }
    return Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1
  }

  private readonly easeInOutElastic = (x: number) => {
    const c5 = (2 * Math.PI) / 4.5
    if (x === 0 || x === 1) {
      return x
    }
    return x < 0.5
      ? -(Math.pow(2, 20 * x - 10) * Math.sin((20 * x - 11.125) * c5)) / 2
      : (Math.pow(2, -20 * x + 10) * Math.sin((20 * x - 11.125) * c5)) / 2 + 1
  }
}
