#!/usr/bin/env bash

# === Configuration ===
JAR_PATH="java/analyzer/target/analyzer-1.0-SNAPSHOT.jar"
PYTHON_SCRIPT="python/visualizer.py"

# === Functions ===

usage() {
    echo "Usage: $0 <input.pgn> <output.png>"
    echo "Example: $0 games.pgn output.png"
    exit 1
}

# === Input Validation ===

if [ "$#" -ne 2 ]; then
    echo "[ERROR] Incorrect number of arguments."
    usage
fi

PGN_FILE="$1"
OUTPUT_FILE="$2"

# Check if PGN file exists
if [ ! -f "$PGN_FILE" ]; then
    echo "[ERROR] PGN file '$PGN_FILE' does not exist."
    exit 2
fi

# Check if PGN file has correct extension
if [[ "$PGN_FILE" != *.pgn ]]; then
    echo "[ERROR] Input file must have a .pgn extension."
    exit 3
fi

# Check if output file has correct extension
if [[ "$OUTPUT_FILE" != *.png ]]; then
    echo "[ERROR] Output file must have a .png extension."
    exit 4
fi

# Check if Java JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "[ERROR] Analyzer JAR not found at '$JAR_PATH'."
    exit 5
fi

# Check if Python script exists
if [ ! -f "$PYTHON_SCRIPT" ]; then
    echo "[ERROR] Python visualizer not found at '$PYTHON_SCRIPT'."
    exit 6
fi

# === Execute Pipeline ===

echo "[INFO] Running analyzer and visualizer..."
java -jar "$JAR_PATH" "$PGN_FILE" | python3 "$PYTHON_SCRIPT" "$OUTPUT_FILE"

if [ $? -ne 0 ]; then
    echo "[FAILURE] An error occurred during analysis or visualization."
    exit 7
fi

