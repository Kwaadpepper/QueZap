import { Pipe, PipeTransform } from '@angular/core'

@Pipe({ name: 'minutes' })
export class MinutesPipe implements PipeTransform {
  transform(seconds: number): string {
    return MinutesPipe.secondsToLabel(seconds)
  }

  public static secondsToLabel(seconds: number): string {
    if (seconds < 60) {
      return `${seconds} sec${seconds > 1 ? 's' : ''}`
    }

    const pluralize = (value: number) => (value > 1 ? 's' : '')
    const mins = Math.floor(seconds / 60)
    const remSecs = seconds % 60
    return remSecs === 0
      ? `${mins} min${pluralize(mins)}`
      : `${mins} min${pluralize(mins)} ${remSecs} sec${pluralize(remSecs)}`
  }
}
