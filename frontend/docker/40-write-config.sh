#!/bin/sh
set -eu

api_url="${API_URL:-}"

printf 'window.APP_CONFIG = { apiUrl: "%s" };\n' "$api_url" > /usr/share/nginx/html/config.js

echo "40-write-config.sh: apiUrl set to '${api_url}'"