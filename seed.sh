#!/bin/bash
set -e

BASE_URL="${BASE_URL:-}"
if [ -z "$BASE_URL" ]; then
  if curl -s -m 2 "http://localhost:8000/apiv1/auth/login" > /dev/null 2>&1; then
    BASE_URL="http://localhost:8000/apiv1"
  else
    BASE_URL="http://localhost:8080/apiv1"
  fi
fi
RAND_SUFFIX=$(( RANDOM % 9000 + 1000 ))

echo "=========================================="
echo "🌱 Seeding QueZap Database via REST API"
echo "📡 Cible : $BASE_URL"
echo "=========================================="

# Check if backend is reachable
if ! curl -s -m 2 "$BASE_URL/auth/login" > /dev/null 2>&1 && ! curl -s -m 2 "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
  echo "❌ Le backend n'est pas accessible sur $BASE_URL"
  echo "👉 Démarre-le d'abord avec l'une des commandes suivantes :"
  echo "   - Mode Docker : docker compose up -d"
  echo "   - Mode Dev    : ./gradlew :quezap-api:bootRun"
  exit 1
fi

# 1. Register or Login Admin User
echo "👤 Authenticating admin user..."
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"admin@quezap.com", "password":"Password123!"}')

TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4 || true)

if [ -z "$TOKEN" ]; then
  echo "⚠️ Admin user not found. Registering new admin user..."
  curl -s -X POST "$BASE_URL/users" \
    -H "Content-Type: application/json" \
    -d '{"name":"Admin", "identifier":"admin@quezap.com", "password":"Password123!"}' || true

  LOGIN_RESP=$(curl -s -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"identifier":"admin@quezap.com", "password":"Password123!"}')

  TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
  echo "❌ Error: Failed to obtain JWT token."
  echo "$LOGIN_RESP"
  exit 1
fi

echo "✅ Authenticated. Token acquired."

# 2. Create Themes with unique names
echo "🎨 Creating Themes..."
THEME1_NAME="Culture G $RAND_SUFFIX"
THEME1_RESP=$(curl -s -X POST "$BASE_URL/themes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"'"$THEME1_NAME"'"}')
THEME1_ID=$(echo "$THEME1_RESP" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4 || echo "$THEME1_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

THEME2_NAME="Sciences $RAND_SUFFIX"
THEME2_RESP=$(curl -s -X POST "$BASE_URL/themes" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"'"$THEME2_NAME"'"}')
THEME2_ID=$(echo "$THEME2_RESP" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4 || echo "$THEME2_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

echo "✅ Themes created: $THEME1_NAME ($THEME1_ID), $THEME2_NAME ($THEME2_ID)"

# 3. Create Questions
echo "❓ Adding Questions..."
Q1_RESP=$(curl -s -X POST "$BASE_URL/questions/affirmation" \
  -H "Authorization: Bearer $TOKEN" \
  -F "question=La Terre met 365 jours et un quart pour faire le tour du Soleil ?" \
  -F "isTrue=true" \
  -F "themeId=$THEME1_ID")
Q1_ID=$(echo "$Q1_RESP" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4 || echo "$Q1_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

Q2_RESP=$(curl -s -X POST "$BASE_URL/questions/affirmation" \
  -H "Authorization: Bearer $TOKEN" \
  -F "question=L'Everest est le sommet le plus haut du monde ?" \
  -F "isTrue=true" \
  -F "themeId=$THEME1_ID")
Q2_ID=$(echo "$Q2_RESP" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4 || echo "$Q2_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

Q3_RESP=$(curl -s -X POST "$BASE_URL/questions/quizz" \
  -H "Authorization: Bearer $TOKEN" \
  -F "question=Quelle est la planète la plus proche du Soleil ?" \
  -F "themeId=$THEME2_ID" \
  -F "answers[0].answer=Mercure" -F "answers[0].isTrue=true" \
  -F "answers[1].answer=Vénus" -F "answers[1].isTrue=false" \
  -F "answers[2].answer=Mars" -F "answers[2].isTrue=false" \
  -F "answers[3].answer=Jupiter" -F "answers[3].isTrue=false")
Q3_ID=$(echo "$Q3_RESP" | grep -o '"uuid":"[^"]*' | cut -d'"' -f4 || echo "$Q3_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

echo "✅ Questions created: ($Q1_ID, $Q2_ID, $Q3_ID)"

# 4. Create Quezap
echo "📦 Creating Quezap Quiz..."
QUEZAP_RESP=$(curl -s -X POST "$BASE_URL/quezaps" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Grand Quiz Découverte '"$RAND_SUFFIX"'",
    "description": "Un quiz dynamique pour tester vos connaissances générales !",
    "isPublic": true,
    "slides": [
      {"questionId": "'"$Q1_ID"'", "timer": 20, "points": 1000},
      {"questionId": "'"$Q2_ID"'", "timer": 20, "points": 1000},
      {"questionId": "'"$Q3_ID"'", "timer": 30, "points": 1500}
    ]
  }')
QUEZAP_ID=$(echo "$QUEZAP_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
echo "✅ Quezap created: ($QUEZAP_ID)"

# 5. Create Active Session
echo "🎮 Creating Live Game Session..."
SESSION_RESP=$(curl -s -X POST "$BASE_URL/sessions" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Session Démo Classe '"$RAND_SUFFIX"'",
    "quezapId": "'"$QUEZAP_ID"'"
  }')
SESSION_ID=$(echo "$SESSION_RESP" | grep -o '"id":"[^"]*' | cut -d'"' -f4)
SESSION_CODE=$(echo "$SESSION_RESP" | grep -o '"code":"[^"]*' | cut -d'"' -f4)

FRONT_PORT="4200"
if curl -s -m 1 "http://localhost:8000" > /dev/null 2>&1; then
  FRONT_PORT="8000"
fi

echo "=========================================="
echo "🎉 SEEDING TERMINÉ AVEC SUCCÈS !"
echo "=========================================="
echo "📌 Admin : admin@quezap.com / Password123!"
echo "📌 Session ID : $SESSION_ID"
echo "📌 Code PIN Session : $SESSION_CODE"
echo "📌 Écran Vidéoprojecteur (Dev: 4200) : http://localhost:4200/admin/sessions/$SESSION_ID/present"
echo "📌 Écran Élève (Dev: 4200)          : http://localhost:4200/quizz/join/$SESSION_CODE"
if [ "$FRONT_PORT" = "8000" ]; then
  echo "📌 Écran Vidéoprojecteur (Docker)   : http://localhost:8000/admin/sessions/$SESSION_ID/present"
  echo "📌 Écran Élève (Docker)             : http://localhost:8000/quizz/join/$SESSION_CODE"
fi
echo "=========================================="
