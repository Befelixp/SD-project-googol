#!/bin/bash

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
    esac
}

# Limpar a tela
clear
print_separator
echo -e "\n🛑 GATEWAY SERVICE TERMINATION\n"
print_separator

# Verificar se o processo do RMIGateway está em execução
echo -e "\n🔍 Searching for RMI Gateway process..."
pid=$(ps aux | grep 'meta1sd.RMIGateway' | grep -v 'grep' | awk '{print $2}')

if [ -z "$pid" ]; then
    print_status "warning" "RMI Gateway is not running"
    print_separator
    echo ""
    exit 0
else
    print_status "info" "Found RMI Gateway process with PID: \033[1;35m$pid\033[0m"
    echo -e "\n⏳ Stopping service..."
    kill $pid
    
    # Aguardar um momento e verificar se o processo foi realmente terminado
    sleep 2
    if ps -p $pid > /dev/null; then
        print_status "error" "Failed to stop RMI Gateway. Try using: kill -9 $pid"
    else
        print_status "success" "RMI Gateway stopped successfully"
    fi
fi

print_separator
echo ""