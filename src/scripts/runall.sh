#!/bin/bash

# Nome do script: startall.sh

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Função para imprimir separador
print_separator() {
    echo -e "\n${BLUE}════════════════════════════════════════${NC}\n"
}

# Função para imprimir mensagem de status
print_status() {
    echo -e "${YELLOW}[$(date +"%Y-%m-%d %H:%M:%S")]${NC} $1"
}

# Função para iniciar um componente
start_component() {
    local component=$1
    local command=$2
    print_status "${GREEN}Iniciando $component...${NC}"
    $command &
    sleep 2
}

# Criar diretório de logs se não existir
mkdir -p ../logs

print_separator
echo -e "${BLUE}🚀 Iniciando Sistema Googol${NC}"
print_separator

# Inicia o Gateway
start_component "Gateway" "./rungateway.sh ../src/config/gateway.properties"
print_status "Gateway iniciado"

# Inicia o Downloader
start_component "Downloader" "./rundownloader.sh 1 ../src/config/downloaders.properties"
print_status "Downloader iniciado"

# Inicia as duas Barrels
start_component "Barrel 1" "./runibs.sh -i 1 ../src/config/indexstoragebarrels.properties"
print_status "Barrel 1 iniciada"
start_component "Barrel 2" "./runibs.sh -i 2 ../src/config/indexstoragebarrels.properties"
print_status "Barrel 2 iniciada"

print_separator
echo -e "${GREEN}✅ Todos os componentes foram iniciados!${NC}"
echo -e "${BLUE}📋 Para verificar os logs:${NC}"
echo "tail -f ../logs/gateway_latest.log"
print_separator

# Aguarda input do usuário para encerrar
echo -e "${YELLOW}⚠️  Pressione CTRL+C para encerrar todos os componentes${NC}"
wait