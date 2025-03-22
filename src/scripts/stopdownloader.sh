#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Encontrar o processo do Downloader
downloader_pid=$(ps aux | grep "meta1sd.Downloader" | grep -v grep | awk '{print $2}')

if [ -z "$downloader_pid" ]; then
    echo "Downloader is not running."
    exit 1
fi

# Parar o processo
echo "Stopping Downloader with PID: $downloader_pid..."
kill "$downloader_pid"

# Verificar se o processo foi encerrado
sleep 2
if ps -p "$downloader_pid" > /dev/null 2>&1; then
    echo "Downloader did not stop gracefully, forcing termination..."
    kill -9 "$downloader_pid"
    sleep 1
fi

echo "Downloader stopped."