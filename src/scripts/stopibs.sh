#!/bin/bash

# Caminho para o diretório de logs onde os PIDs são armazenados
project_root=$(dirname $(dirname $(realpath $0)))
pid_dir="$project_root/logs"

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

# Função para parar uma barrel específica
stop_barrel() {
    local barrel_id=$1
    local pid_file="$pid_dir/ibs_${barrel_id}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            print_status "info" "Stopping Index Storage Barrel \033[1;36m$barrel_id\033[0m (PID: \033[1;35m$pid\033[0m)..."
            kill "$pid"
            sleep 2
            if ps -p $pid > /dev/null 2>&1; then
                print_status "warning" "Process $pid did not stop gracefully, forcing termination..."
                kill -9 "$pid"
            fi
            rm "$pid_file"
            print_status "success" "Index Storage Barrel $barrel_id stopped successfully."
        else
            print_status "warning" "Index Storage Barrel $barrel_id (PID: $pid) is not running."
            rm "$pid_file"
        fi
    else
        print_status "error" "No PID file found for Index Storage Barrel $barrel_id."
    fi
}

# Limpar a tela
clear
print_separator
echo -e "\n🛑 INDEX STORAGE BARREL TERMINATION\n"
print_separator

# Verificar se um ID específico foi fornecido
if [ $# -eq 1 ]; then
    # Parar uma barrel específica
    print_status "info" "Stopping specific Index Storage Barrel with ID: \033[1;36m$1\033[0m"
    stop_barrel "$1"
else
    # Parar todas as barrels em execução
    print_status "info" "Stopping all Index Storage Barrels..."
    pids=$(ps aux | grep 'meta1sd.IndexStorageBarrel' | grep -v 'grep' | awk '{print $2}')
    
    if [ -z "$pids" ]; then
        print_status "warning" "No Index Storage Barrels are running."
    else
        for pid in $pids; do
            print_status "info" "Stopping process with PID: \033[1;35m$pid\033[0m..."
            kill "$pid"
            sleep 2
            if ps -p $pid > /dev/null 2>&1; then
                print_status "warning" "Process $pid did not stop gracefully, forcing termination..."
                kill -9 "$pid"
            fi
        done
        # Limpar arquivos PID
        rm -f "$pid_dir"/ibs_*.pid 2>/dev/null
        print_status "success" "All Index Storage Barrels stopped successfully."
    fi
fi

print_separator
echo ""