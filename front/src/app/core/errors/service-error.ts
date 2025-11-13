export abstract class ServiceError extends Error {
  public static override readonly name: string
  public static readonly code: number
}
