import { z } from 'zod'

z.config(z.locales.fr())

// export configured zod instance
export { z as zod } from 'zod'
