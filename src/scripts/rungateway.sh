#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Caminho padrão para o arquivo de propriedades
input_path="$project_root/config/gateway.properties"

# Caminho padrão para o arquivo de log
output_path="$project_root/logs/gateway_$(date +%Y%m%d%H%M%S%N).txt"

# Se o usuário fornecer um arquivo de propriedades, use-o
if [ $# -gt 0 ]; then
    if [ -f $1 ]; then
        if [[ $1 == *.properties ]]; then
            input_path=$1
        else
            echo "The input file must be a properties file."
            exit 1
        fi
    else
        echo "The input file does not exist."
        exit 1
    fi
fi

# Se o usuário fornecer um caminho de saída, use-o
if [ $# -gt 1 ]; then
    output_path=$2
fi

# Criar o diretório de logs, se não existir
mkdir -p $(dirname $output_path)

# Informações de execução
echo "Starting RMI Gateway with:"
echo "- Properties file: $input_path"
echo "- Log file: $output_path"

# Verificar se o arquivo de propriedades existe
if [ ! -f "$input_path" ]; then
    echo "Error: Properties file $input_path does not exist!"
    exit 1
fi

# Executar o RMIGateway
cd "$project_root/bin"
java -cp .. meta1sd.RMIGateway "$input_path" > "$output_path" 2>&1 &

# Obter o PID do processo
gateway_pid=$!
echo "RMI Gateway started with PID: $gateway_pid"
echo "To view logs: tail -f $output_path"