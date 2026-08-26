#!/bin/bash

# SyntricDB Installer Script with Credentials & Interactive CLI Setup
set -e

SYNTRICDB_DIR="$HOME/.syntricdb"
INSTALL_DIR="$SYNTRICDB_DIR/bin"
CONF_FILE="$SYNTRICDB_DIR/syntricdb.conf"
JAR_PATH="$(pwd)/target/syntricdb-engine-1.0.0-SNAPSHOT.jar"

echo "=========================================================================="
echo "⚡ SyntricDB AI-Native Database Enterprise Installer ⚡"
echo "=========================================================================="

mkdir -p "$SYNTRICDB_DIR" "$INSTALL_DIR"

# Interactive Credential Setup if not passed via env
if [ -t 0 ] && [ -z "$SYNTRICDB_NON_INTERACTIVE" ]; then
    echo "🔐 Setting up Database Administrator Credentials:"
    read -p "   • Admin Username [default: admin]: " ADMIN_USER
    ADMIN_USER=${ADMIN_USER:-admin}

    read -sp "   • Admin Password [default: syntricdb_secret_pass]: " ADMIN_PASS
    echo ""
    ADMIN_PASS=${ADMIN_PASS:-syntricdb_secret_pass}

    read -p "   • Database Port [default: 8080]: " ADMIN_PORT
    ADMIN_PORT=${ADMIN_PORT:-8080}
else
    ADMIN_USER=${SYNTRICDB_ADMIN_USER:-admin}
    ADMIN_PASS=${SYNTRICDB_ADMIN_PASSWORD:-syntricdb_secret_pass}
    ADMIN_PORT=${SYNTRICDB_PORT:-8080}
fi

# Write Configuration
cat << EOF > "$CONF_FILE"
bind_address=0.0.0.0
port=$ADMIN_PORT
auth_enabled=true
admin_user=$ADMIN_USER
admin_password=$ADMIN_PASS
data_dir=$SYNTRICDB_DIR/data
wal_dir=$SYNTRICDB_DIR/wal
snapshot_dir=$SYNTRICDB_DIR/snapshots
EOF

echo "✅ Saved configuration to $CONF_FILE"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building SyntricDB production JAR..."
    mvn clean package -DskipTests
fi

cp "$JAR_PATH" "$INSTALL_DIR/syntricdb-engine.jar"

# Create launcher script
LAUNCHER="$INSTALL_DIR/syntricdb"

cat << 'EOF' > "$LAUNCHER"
#!/bin/bash

SYNTRICDB_DIR="$HOME/.syntricdb"
SYNTRICDB_JAR="$SYNTRICDB_DIR/bin/syntricdb-engine.jar"
CONF_FILE="$SYNTRICDB_DIR/syntricdb.conf"
PID_FILE="$SYNTRICDB_DIR/syntricdb.pid"
LOG_FILE="$SYNTRICDB_DIR/syntricdb.log"

# Load config variables if present
if [ -f "$CONF_FILE" ]; then
    source "$CONF_FILE" 2>/dev/null || true
fi

PORT=${port:-8080}
USER=${admin_user:-admin}
PASS=${admin_password:-syntricdb_secret_pass}

case "$1" in
    start|server)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "⚡ SyntricDB Server is already running (PID: $(cat "$PID_FILE"))"
            echo "🌐 Web Studio: http://localhost:$PORT/"
            exit 0
        fi
        echo "🚀 Starting SyntricDB Server on port $PORT..."
        nohup java -jar "$SYNTRICDB_JAR" > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 2
        echo "✅ SyntricDB Server started successfully (PID: $(cat "$PID_FILE"))"
        echo "🔑 Admin User    : $USER"
        echo "🌐 Web Console  : http://localhost:$PORT/"
        echo "📡 REST API     : http://localhost:$PORT/api/sql"
        echo "🔗 Connection String: jdbc:syntricdb://$USER:*****@localhost:$PORT/default"
        ;;
    stop)
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            echo "🛑 Stopping SyntricDB Server (PID: $PID)..."
            kill "$PID" 2>/dev/null || true
            rm -f "$PID_FILE"
            echo "✅ SyntricDB Server stopped."
        else
            echo "⚠️ SyntricDB Server is not running."
        fi
        ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "🟢 SyntricDB Server is running (PID: $(cat "$PID_FILE"))"
            echo "🌐 Web Console: http://localhost:$PORT/"
        else
            echo "🔴 SyntricDB Server is stopped."
        fi
        ;;
    cli|shell)
        shift 1
        java -cp "$SYNTRICDB_JAR" com.syntricdb.cli.SyntricCLI -u "$USER" -p "$PASS" -h "http://localhost:$PORT" "$@"
        ;;
    logs)
        tail -f "$LOG_FILE"
        ;;
    *)
        echo "=========================================================="
        echo "⚡ SyntricDB: Next-Generation AI-Native Unified Database ⚡"
        echo "=========================================================="
        echo "Usage: syntricdb {start|stop|status|cli|logs}"
        echo "  syntricdb start   : Launch background server daemon (Port $PORT)"
        echo "  syntricdb stop    : Shutdown background server daemon"
        echo "  syntricdb status  : Check server status and endpoint info"
        echo "  syntricdb cli     : Launch interactive SQL & Vector shell"
        echo "  syntricdb logs    : Tail server stdout/stderr logs"
        echo "=========================================================="
        ;;
esac
EOF

chmod +x "$LAUNCHER"

# Attempt to link into /usr/local/bin or advise path addition
if [ -d "/usr/local/bin" ] && [ -w "/usr/local/bin" ]; then
    ln -sf "$LAUNCHER" /usr/local/bin/syntricdb
    echo "✅ Executable linked to /usr/local/bin/syntricdb"
else
    echo "✅ Executable installed to $LAUNCHER"
    echo "💡 Add to PATH: export PATH=\"\$HOME/.syntricdb/bin:\$PATH\""
fi

echo ""
echo "=========================================================================="
echo "🎉 SyntricDB Installation Complete!"
echo "=========================================================================="
echo "🔑 Connection String: jdbc:syntricdb://$ADMIN_USER:$ADMIN_PASS@localhost:$ADMIN_PORT/default"
echo "🚀 Start Database   : syntricdb start"
echo "💻 Launch CLI Shell : syntricdb cli"
echo "=========================================================================="
