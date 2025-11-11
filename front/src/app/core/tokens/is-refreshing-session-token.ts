import { HttpContextToken } from '@angular/common/http'

const IS_REFRESHING_ACCESS_TOKEN = new HttpContextToken<boolean>(() => false)

export { IS_REFRESHING_ACCESS_TOKEN }
