#!/bin/bash

# Nome do script: stopall.sh

# Cores para output
RED='\033[0;31m'
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

print_separator
echo -e "${RED}🛑 Parando todos os componentes do sistema...${NC}"
print_separator

# Parar o Gateway
print_status "${RED}Parando Gateway...${NC}"
./stopgateway.sh
print_status "Gateway parado"

# Parar o Downloader
print_status "${RED}Parando Downloader...${NC}"
./stopdownloader.sh
print_status "Downloader parado"

# Parar as Barrels
print_status "${RED}Parando Barrels...${NC}"
./stopibs.sh
print_status "Barrels paradas"

print_separator
echo -e "${RED}✅ Todos os componentes foram parados!${NC}"
print_separator