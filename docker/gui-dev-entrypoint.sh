#!/bin/sh
# Watch-mode entrypoint for the GUI container.
#  - npm install on first start (volume empty)
#  - patch environment.ts to use relative URLs (same as the prod nginx routing)
#  - patch flow-config.ts to drop the falsy-host ternary
#  - run `ng serve` with proxy.conf.json
set -eu

cd /app

if [ ! -d node_modules ] || [ -z "$(ls -A node_modules 2>/dev/null)" ]; then
  echo "[gui-dev] First start — running npm install (a few minutes)..."
  npm install --force
fi

# Same patches as the prod gui.Dockerfile, applied at runtime here.
sed -i "s|'http://localhost:'|''|g" src/environments/environment.ts
sed -i "s|'8080/|'/|g" src/environments/environment.ts
sed -i "s|'8081/|'/|g" src/environments/environment.ts
sed -i "s|env\.host ? \`\${env\.host}\${env\.apis\.upload\.upload}\` : ''|\`\${env.host}\${env.apis.upload.upload}\`|g" \
  src/app/shared/config/flow-config.ts

exec npx ng serve --host 0.0.0.0 --port 4200 --proxy-config proxy.conf.json --poll 2000 --disable-host-check
