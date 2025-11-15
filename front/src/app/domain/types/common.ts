interface UUIDBrand {
  readonly UUID: unique symbol
  readonly __type: 'UUID'
}

interface JWTBrand {
  readonly __type: 'JWT'
}

export type UUID = `${string}-${string}-${string}-${string}-${string}` & UUIDBrand
export type JWT = string & JWTBrand
