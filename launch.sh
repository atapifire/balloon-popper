#!/bin/bash
# Launch script for custom RuneLite client with Jagex account session

# Load environment variables from .env file
if [ -f "$(dirname "$0")/.env" ]; then
    set -a
    source "$(dirname "$0")/.env"
    set +a
else
    echo "Error: .env file not found"
    exit 1
fi

# Display launch information
echo "========================================="
echo "Launching Custom RuneLite Client"
echo "========================================="
echo "Character: $JX_DISPLAY_NAME"
echo "Character ID: $JX_CHARACTER_ID"
echo "JAR Path: $CUSTOM_JAR_PATH"
echo "========================================="

# Check if JAR exists
if [ ! -f "$CUSTOM_JAR_PATH" ]; then
    echo "Error: Custom JAR not found at $CUSTOM_JAR_PATH"
    exit 1
fi

# Launch the client with optimizations and JX session info
_JAVA_OPTIONS="$JAVA_OPTS" \
__GL_SYNC_TO_VBLANK=$GL_SYNC_TO_VBLANK \
__GL_THREADED_OPTIMIZATIONS=$GL_THREADED_OPTIMIZATIONS \
JX_SESSION_ID="$JX_SESSION_ID" \
JX_CHARACTER_ID="$JX_CHARACTER_ID" \
JX_DISPLAY_NAME="$JX_DISPLAY_NAME" \
java -jar "$CUSTOM_JAR_PATH" --developer-mode "$@"
