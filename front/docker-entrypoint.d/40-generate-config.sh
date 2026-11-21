#!/bin/sh
# Generates /usr/share/nginx/html/config.json from container environment variables
# so pre-built frontend images can be configured at runtime without rebuilding.
set -eu

CONFIG_PATH="/usr/share/nginx/html/config.json"

json_escape() {
  printf '%s' "$1" | awk '
    BEGIN { ORS = "" }
    {
      if (NR > 1) printf "\\n"
      gsub(/\\/, "\\\\\\\\")
      gsub(/"/, "\\\"")
      gsub(/\t/, "\\t")
      gsub(/\r/, "")
      print
    }
  '
}

first=1
{
  printf '{\n'

  add_field() {
    key="$1"
    val="$2"
    if [ -n "$val" ]; then
      escaped="$(json_escape "$val")"
      if [ "$first" -eq 0 ]; then
        printf ',\n'
      fi
      printf '  "%s": "%s"' "$key" "$escaped"
      first=0
    fi
  }

  add_field "appName" "${APP_NAME:-}"
  add_field "authorName" "${APP_AUTHOR_NAME:-}"
  add_field "authorEmail" "${APP_AUTHOR_EMAIL:-}"
  add_field "apiUrl" "${APP_API_URL:-}"
  add_field "turnstileSiteKey" "${APP_TURNSTILE_SITE_KEY:-}"

  printf '\n}\n'
} > "$CONFIG_PATH"
