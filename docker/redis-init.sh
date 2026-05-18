#!/bin/sh
set -eu

# Pre-seed Redis with the "Ignimission" mail domain set used by the upload-api
# to decide whether a sender is a public agent (link feature, public link...).
# In prod this set is populated by a scheduled Ignimission sync; locally we
# hardcode a few common .gouv.fr domains + the local mail domain.

echo "Seeding Redis SET enclosure-mails:mails with state domains..."

DOMAINS="
gouv.fr
culture.gouv.fr
interieur.gouv.fr
numerique.gouv.fr
economie.gouv.fr
education.gouv.fr
agriculture.gouv.fr
defense.gouv.fr
diplomatie.gouv.fr
finances.gouv.fr
justice.gouv.fr
sante.gouv.fr
transports.gouv.fr
travail.gouv.fr
modernisation.gouv.fr
ecologie.gouv.fr
francetransfert.local
example.gouv.fr
"

for d in $DOMAINS; do
  redis-cli -h redis -a "$METALOAD_PASSWORD" --no-auth-warning SADD enclosure-mails:mails "$d" >/dev/null
done
echo "Done. Total domains in set:"
redis-cli -h redis -a "$METALOAD_PASSWORD" --no-auth-warning SCARD enclosure-mails:mails

echo
echo "Tip: to add your own domain at runtime, run:"
echo "  docker compose --env-file .env.local exec redis \\"
echo "    redis-cli -a \"\$METALOAD_PASSWORD\" --no-auth-warning SADD enclosure-mails:mails YOUR_DOMAIN.gouv.fr"
