#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Definir variáveis de ambiente e caminhos
JAVA_OPTS="-Xmx512m -Xms256m"
CLASSPATH="$project_root/target:$project_root/libs/jars/*"

# Caminhos padrão
input_path="$project_root/config/gateway.properties"
output_path="$project_root/logs/gateway_$(date +%Y%m%d%H%M%S).log"

# Função para imprimir linha separadora
print_separator() {
    echo "═══════════════════════════════════════════════════════════════════════════"
}

# Função para validar arquivo de propriedades
validate_properties_file() {
    local file=$1
    if [ ! -f "$file" ]; then
        echo "❌ Error: Properties file $file does not exist!"
        exit 1
    fi
    if [ "$(echo "$file" | grep '\.properties$')" = "" ]; then
        echo "❌ Error: File must have .properties extension!"
        exit 1
    fi
}

# Processar argumentos da linha de comando
while getopts ":p:o:h" opt; do
    case $opt in
        p) input_path="$OPTARG"
           ;;
        o) output_path="$OPTARG"
           ;;
        h) echo -e "\n📋 Usage: $0 [-p properties_file] [-o output_file]"
           echo "  -p: Properties file path (default: config/gateway.properties)"
           echo "  -o: Output log file path"
           echo "  -h: Show this help message"
           echo ""
           exit 0
           ;;
        \?) echo "❌ Invalid option -$OPTARG"
            exit 1
            ;;
    esac
done

# Validar arquivo de propriedades
validate_properties_file "$input_path"

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$output_path")"

# Limpar a tela e mostrar informações de execução
clear
print_separator
echo -e "\n🌐 RMI GATEWAY INITIALIZATION\n"
print_separator
echo -e "\n📋 Configuration Details:"
echo -e "  • Properties File: \033[1;33m$input_path\033[0m"
echo -e "  • Log File: \033[1;33m$output_path\033[0m"
echo -e "  • Classpath: \033[0;32m$CLASSPATH\033[0m\n"
print_separator

# Executar o RMIGateway
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.RMIGateway "$input_path" > "$output_path" 2>&1 &
PID=$!

# Imprimir informações do processo
echo -e "\n🚀 Gateway Service Started Successfully!"
echo -e "📌 Process ID (PID): \033[1;35m$PID\033[0m"
echo -e "💡 To stop the gateway, use: \033[1;31mkill $PID\033[0m\n"
print_separator
echo ""

# Aguardar um momento para verificar se o processo iniciou corretamente
sleep 2
if ps -p $PID > /dev/null; then
    echo -e "✅ Gateway is running properly\n"
else
    echo -e "❌ Gateway failed to start. Check logs at: $output_path\n"
fi
print_separator
echo ""