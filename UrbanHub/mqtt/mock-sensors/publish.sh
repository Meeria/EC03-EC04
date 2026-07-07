#!/bin/sh
set -e

BROKER="${MQTT_BROKER:-mosquitto}"
PORT="${MQTT_PORT:-1883}"
INTERVAL="${PUBLISH_INTERVAL:-5}"
TOPIC_PREFIX="urbanhub/sensors"
LOCATIONS_FILE="/data/caen-locations.json"
SENSORS_PER_TYPE=500

echo "Waiting for broker $BROKER:$PORT ..."
until mosquitto_pub -h "$BROKER" -p "$PORT" -t "ping" -m "hello" -q 0 2>/dev/null; do
  sleep 1
done
echo "Broker ready – $((SENSORS_PER_TYPE * 4)) sensors, publishing every ${INTERVAL}s"

# Pre-extract locations into flat arrays (line N = location N)
# Types get consecutive slices: air=0-499, noise=500-999, traffic=1000-1499, weather=1500-1999
LATS=$(python3 -c "
import json
locs = json.load(open('$LOCATIONS_FILE'))
for l in locs:
    print(l['lat'])
")
LONS=$(python3 -c "
import json
locs = json.load(open('$LOCATIONS_FILE'))
for l in locs:
    print(l['lon'])
")

# Store in temp files for fast line-based access
echo "$LATS" > /tmp/lats.txt
echo "$LONS" > /tmp/lons.txt

get_location() {
  line=$((${1} + 1))
  lat=$(sed -n "${line}p" /tmp/lats.txt)
  lon=$(sed -n "${line}p" /tmp/lons.txt)
  echo "${lat};${lon}"
}

rand_float() {
  awk -v min="$1" -v max="$2" -v seed="$RANDOM" 'BEGIN { srand(seed); printf "%.1f", min + rand() * (max - min) }'
}

rand_int() {
  awk -v min="$1" -v max="$2" -v seed="$RANDOM" 'BEGIN { srand(seed); printf "%d", min + rand() * (max - min) }'
}

publish_sensor() {
  type=$1 idx=$2 loc_offset=$3 val=$4 unit=$5
  loc_index=$((loc_offset + idx))
  loc=$(get_location "$loc_index")
  sid=$(printf "sensor-%s-%03d" "$type" "$((idx + 1))")
  mosquitto_pub -h "$BROKER" -p "$PORT" -t "$TOPIC_PREFIX/$type/$sid" -m \
    "{\"sensor_id\":\"$sid\",\"type\":\"$type\",\"timestamp\":\"$TS\",\"location\":\"$loc\",\"value\":$val,\"unit\":\"$unit\"}"
}

while true; do
  TS=$(($(date -u +%s) * 1000))

  i=0
  while [ "$i" -lt "$SENSORS_PER_TYPE" ]; do
    publish_sensor "air"     "$i" 0    "$(rand_float 5 80)"   "μg/m3"
    publish_sensor "noise"   "$i" 500  "$(rand_float 30 90)"  "dB"
    publish_sensor "traffic" "$i" 1000 "$(rand_int 0 120)"    "km/h"
    publish_sensor "weather" "$i" 1500 "$(rand_float -5 35)"  "°C"
    i=$((i + 1))
  done

  echo "[$TS] Published $((SENSORS_PER_TYPE * 4)) measures"
  sleep "$INTERVAL"
done
