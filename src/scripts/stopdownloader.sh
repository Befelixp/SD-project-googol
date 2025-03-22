#!/bin/sh

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Função para verificar se um PID é válido
is_valid_pid() {
    local pid=$1
    case "$pid" in
        ''|*[!0-9]*) return 1 ;; # Retorna falso se não for número
        *) kill -0 "$pid" 2>/dev/null; return $? ;;
    esac
}

# Encontrar todos os processos do Downloader
downloader_pids=$(ps aux | grep "meta1sd.Downloader" | grep -v grep | awk '{print $2}')

if [ -z "$downloader_pids" ]; then
    echo "No Downloader processes are running."
    exit 0
fi

# Tentar parar cada processo encontrado
for pid in $downloader_pids; do
    if is_valid_pid "$pid"; then
        echo "Stopping Downloader with PID: $pid..."
        kill "$pid"

        # Aguardar até 5 segundos pelo encerramento gracioso
        count=0
        while [ $count -lt 5 ] && ps -p "$pid" > /dev/null 2>&1; do
            sleep 1
            count=$((count + 1))
        done

        # Se ainda estiver rodando, força o encerramento
        if ps -p "$pid" > /dev/null 2>&1; then
            echo "Process $pid did not stop gracefully, forcing termination..."
            kill -9 "$pid"
            sleep 1
        fi

        # Verificação final
        if ! ps -p "$pid" > /dev/null 2>&1; then
            echo "Process $pid successfully stopped."
        else
            echo "Failed to stop process $pid!"
        fi
    else
        echo "Invalid or non-existent PID: $pid"
    fi
done

# Verificação final de todos os processos
remaining_pids=$(ps aux | grep "meta1sd.Downloader" | grep -v grep | awk '{print $2}')
if [ -z "$remaining_pids" ]; then
    echo "All Downloader processes have been stopped."
else
    echo "Warning: Some Downloader processes may still be running: $remaining_pids"
fi