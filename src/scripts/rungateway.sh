#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Definir variáveis de ambiente e caminhos
JAVA_OPTS="-Xmx512m -Xms256m"
CLASSPATH="$project_root/target:$project_root/libs/jars/*"

# Caminhos padrão
input_path="$project_root/config/gateway.properties"
output_path="$project_root/logs/gateway_$(date +%Y%m%d%H%M%S).log"
pid_file="$project_root/gateway.pid"

# Função para validar arquivo de propriedades
validate_properties_file() {
    local file=$1
    if [ ! -f "$file" ]; then
        echo "Error: Properties file $file does not exist!"
        exit 1
    fi
    if [[ ! $file =~ \.properties$ ]]; then
        echo "Error: File must have .properties extension!"
        exit 1
    fi
}

# Função para verificar se o gateway já está rodando
check_running() {
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo "Error: Gateway is already running with PID $pid"
            exit 1
        else
            rm "$pid_file"
        fi
    fi
}

# Processar argumentos da linha de comando
while getopts ":p:o:h" opt; do
    case $opt in
        p) input_path="$OPTARG"
           ;;
        o) output_path="$OPTARG"
           ;;
        h) echo "Usage: $0 [-p properties_file] [-o output_file]"
           exit 0
           ;;
        \?) echo "Invalid option -$OPTARG"
            exit 1
            ;;
    esac
done

# Validar arquivo de propriedades
validate_properties_file "$input_path"

# Verificar se já está rodando
check_running

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$output_path")"

# Informações de execução
echo "Starting RMI Gateway with:"
echo "- Properties file: $input_path"
echo "- Log file: $output_path"
echo "- Classpath: $CLASSPATH"

# Executar o RMIGateway
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.RMIGateway "$input_path" > "$output_path" 2>&1 &

# Obter e salvar o PID
gateway_pid=$!
echo $gateway_pid > "$pid_file"

# Verificar se o processo iniciou corretamente
sleep 2
if ps -p $gateway_pid > /dev/null 2>&1; then
    echo "RMI Gateway started successfully with PID: $gateway_pid"
    echo "To view logs: tail -f $output_path"
else
    echo "Error: Failed to start RMI Gateway"
    rm -f "$pid_file"
    exit 1
fi

# Função para limpar ao sair
cleanup() {
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        kill $pid 2>/dev/null
        rm -f "$pid_file"
    fi
}
