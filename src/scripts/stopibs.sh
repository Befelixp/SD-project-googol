#!/bin/bash

# Caminho para o diretório de logs onde os PIDs são armazenados
project_root=$(dirname $(dirname $(realpath $0)))
pid_dir="$project_root/logs"

# Função para parar uma barrel específica
stop_barrel() {
    local barrel_id=$1
    local pid_file="$pid_dir/ibs_${barrel_id}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo "Stopping Index Storage Barrel $barrel_id (PID: $pid)"
            kill $pid
            rm "$pid_file"
            echo "Index Storage Barrel $barrel_id stopped."
        else
            echo "Index Storage Barrel $barrel_id (PID: $pid) is not running."
            rm "$pid_file"
        fi
    fi
}

# Verificar se um ID específico foi fornecido
if [ $# -eq 1 ]; then
    # Parar uma barrel específica
    stop_barrel $1
else
    # Parar todas as barrels em execução
    # Procurar por processos IndexStorageBarrel
    pids=$(ps aux | grep 'meta1sd.IndexStorageBarrel' | grep -v 'grep' | awk '{print $2}')
    
    if [ -z "$pids" ]; then
        echo "No Index Storage Barrels are running."
    else
        echo "Stopping all Index Storage Barrels..."
        for pid in $pids; do
            echo "Stopping process with PID: $pid"
            kill $pid
        done
        # Limpar arquivos PID
        rm -f "$pid_dir"/ibs_*.pid 2>/dev/null
        echo "All Index Storage Barrels stopped."
    fi
fi