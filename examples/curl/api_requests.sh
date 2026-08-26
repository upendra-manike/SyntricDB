#!/usr/bin/env bash

# SyntricDB Connection String: syntricdb://admin:syntricdb_secret_pass@localhost:8080/default
SYNTRICDB_HOST="http://localhost:8080/api/sql"
ADMIN_USER="admin"
ADMIN_PASS="syntricdb_secret_pass"

echo "================================================="
echo "💻 SyntricDB cURL & REST API Executable Demo"
echo "🔗 Connection String: syntricdb://$ADMIN_USER:$ADMIN_PASS@localhost:8080/default"
echo "================================================="

echo "1. Creating Table 'curl_logs'..."
curl -s -u "$ADMIN_USER:$ADMIN_PASS" -X POST "$SYNTRICDB_HOST" \
  -H "Content-Type: application/json" \
  -d '{"database": "default", "sql": "CREATE TABLE curl_logs (id VARCHAR PRIMARY KEY, log_level VARCHAR, message VARCHAR, embedding FLOAT_VECTOR(128));"}'
echo -e "\n"

echo "2. Inserting Record with Vector Embedding..."
curl -s -u "$ADMIN_USER:$ADMIN_PASS" -X POST "$SYNTRICDB_HOST" \
  -H "Content-Type: application/json" \
  -d '{"database": "default", "sql": "INSERT INTO curl_logs VALUES (\"log_901\", \"ERROR\", \"Memory allocation failure in worker thread\", AI_EMBED(\"memory allocation error failure thread\"));"}'
echo -e "\n"

echo "3. Querying Hybrid SQL + Vector Similarity..."
curl -s -u "$ADMIN_USER:$ADMIN_PASS" -X POST "$SYNTRICDB_HOST" \
  -H "Content-Type: application/json" \
  -d '{"database": "default", "sql": "SELECT id, log_level, message FROM curl_logs WHERE log_level=\"ERROR\" AND embedding SIMILAR TO \"memory allocation failure\" TOP 1;"}'
echo -e "\n"

echo "================================================="
