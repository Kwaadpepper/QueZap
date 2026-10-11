import { defineConfig } from '@hey-api/openapi-ts'

export default defineConfig({
  input: 'openapi.json',
  output: 'src/app/core/api-client',
  plugins: [
    '@hey-api/typescript',
    '@hey-api/client-angular',
    '@hey-api/sdk',
    'zod',
  ],
})
