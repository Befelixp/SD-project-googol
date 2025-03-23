#!/bin/sh

# Função para imprimir linha separadora
print_separator() {
    echo "═══════════════════════════════════════════════════════════════════════════"
}

# Função para imprimir mensagens de status
print_status() {
    local status=$1
    local message=$2
    case $status in
        "info")    echo -e "ℹ️  $message" ;;
        "success") echo -e "✅ $message" ;;
        "error")   echo -e "❌ $message" ;;
        "warning") echo -e "⚠️  $message" ;;
        "start")   echo -e "🚀 $message" ;;
    esac
}

# Limpar a tela
clear
print_separator
echo -e "\n🛑 DOWNLOADER SERVICE TERMINATION\n"
print_separator

# Função para verificar se um PID é válido
is_valid_pid() {
    local pid=$1
    case "$pid" in
        ''|*[!0-9]*) return 1 ;; # Retorna falso se não for número
        *) kill -0 "$pid" 2>/dev/null; return $? ;;
    esac
}

# Encontrar todos os processos do Downloader
print_status "info" "Searching for Downloader processes..."
downloader_pids=$(ps aux | grep "meta1sd.Downloader" | grep -v grep | awk '{print $2}')

if [ -z "$downloader_pids" ]; then
    print_status "warning" "No Downloader processes are running."
    print_separator
    echo ""
    exit 0
fi

# Tentar parar cada processo encontrado
for pid in $downloader_pids; do
    if is_valid_pid "$pid"; then
        print_status "info" "Stopping Downloader with PID: \033[1;35m$pid\033[0m..."
        kill "$pid"

        # Aguardar até 5 segundos pelo encerramento gracioso
        count=0
        while [ $count -lt 5 ] && ps -p "$pid" > /dev/null 2>&1; do
            sleep 1
            count=$((count + 1))
        done

        # Se ainda estiver rodando, força o encerramento
        if ps -p "$pid" > /dev/null 2>&1; then
            print_status "warning" "Process $pid did not stop gracefully, forcing termination..."
            kill -9 "$pid"
            sleep 1
        fi

        # Verificação final
        if ! ps -p "$pid" > /dev/null 2>&1; then
            print_status "success" "Process $pid successfully stopped."
        else
            print_status "error" "Failed to stop process $pid!"
        fi
    else
        print_status "error" "Invalid or non-existent PID: $pid"
    fi
done

# Verificação final de todos os processos
remaining_pids=$(ps aux | grep "meta1sd.Downloader" | grep -v grep | awk '{print $2}')
if [ -z "$remaining_pids" ]; then
    print_status "success" "All Downloader processes have been stopped."
else
    print_status "warning" "Some Downloader processes may still be running: \033[1;33m$remaining_pids\033[0m"
fi

print_separator
echo ""