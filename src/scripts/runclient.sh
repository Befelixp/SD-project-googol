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
        "start")   echo -e "🚀 $message" ;;
    esac
}

# Limpar a tela
clear
print_separator
echo -e "\n🤝 RMI CLIENT INITIALIZATION\n"
print_separator

# Obter o diretório base do projeto
project_root=$(dirname $(dirname $(realpath $0)))
print_status "info" "Project root directory: \033[1;34m$project_root\033[0m"

# Configurar valores padrão
id=$(date +%N) # ID padrão baseado no timestamp
input_path="config/client.properties"

# Configurar o CLASSPATH corretamente
CLASSPATH="$project_root/target:$project_root/libs/jars/*"
print_status "info" "Classpath configured: \033[0;32m$CLASSPATH\033[0m"

# Processar argumentos da linha de comando
if [ $# -gt 0 ]; then
    if [[ $1 == *[^0-9]* ]]; then 
        print_status "error" "ID must be a number!"
        exit 1
    else
        id=$1
        print_status "info" "Using provided ID: \033[1;36m$id\033[0m"
    fi
fi

if [ $# -gt 1 ]; then
    if [ -f $2 ]; then
        if [[ $2 == *.properties ]]; then
            input_path=$2
            print_status "info" "Using provided properties file: \033[1;33m$input_path\033[0m"
        else
            print_status "error" "The file must have a .properties extension!"
            exit 1
        fi
    else
        print_status "error" "The file does not exist: \033[1;31m$2\033[0m"
        exit 1
    fi
else
    print_status "info" "Using default properties file: \033[1;33m$input_path\033[0m"
fi

# Executar com o CLASSPATH correto
print_separator
print_status "start" "Starting RMI Client with:"
echo -e "  • ID: \033[1;36m$id\033[0m"
echo -e "  • Properties File: \033[1;33m$input_path\033[0m"
echo -e "  • Classpath: \033[0;32m$CLASSPATH\033[0m"
print_separator

cd "$project_root"
java -cp "$CLASSPATH" meta1sd.RMIClient "$id" "$input_path"

if [ $? -eq 0 ]; then
    print_status "success" "RMI Client executed successfully!"
else
    print_status "error" "RMI Client execution failed!"
fi

print_separator
echo ""