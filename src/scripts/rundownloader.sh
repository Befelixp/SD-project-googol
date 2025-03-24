#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Definir variáveis de ambiente e caminhos
JAVA_OPTS="-Xmx512m -Xms256m"
CLASSPATH="$project_root/target:$project_root/libs/jars/*"

# Função para imprimir linha separadora
print_separator() {
    echo "═══════════════════════════════════════════════════════════════════════════"
}

# Função para formatar o nome do arquivo de log
format_log_filename() {
    local id=$1
    local timestamp=$(date +%Y%m%d_%H%M%S)
    echo "$project_root/logs/downloader_${id}_${timestamp}.log"
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

# Verificar se foi fornecido um ID como primeiro argumento
if [ -z "$1" ]; then
    echo "❌ Error: Downloader ID must be provided as first argument!"
    echo "Usage: $0 <id> [-p properties_file] [-o output_file]"
    exit 1
fi

id=$1
# Formatar o nome do arquivo de log com o ID
output_path=$(format_log_filename "$id")
shift # Remove o primeiro argumento (ID) para processar as outras opções

# Caminhos padrão
input_path="$project_root/config/downloaders.properties"

# Processar argumentos da linha de comando
while getopts ":p:o:h" opt; do
    case $opt in
        p) input_path="$OPTARG"
           ;;
        o) output_path="$OPTARG"
           ;;
        h) echo "Usage: $0 <id> [-p properties_file] [-o output_file]"
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

# Informações de execução
print_separator
echo -e "\n📥 DOWNLOADER SERVICE INITIALIZATION\n"
print_separator
echo -e "\n📋 Configuration Details:"
echo -e "  • Downloader ID: \033[1;36m$id\033[0m"
echo -e "  • Properties File: \033[1;33m$input_path\033[0m"
echo -e "  • Log File: \033[1;33m$output_path\033[0m"
echo -e "  • Classpath: \033[0;32m$CLASSPATH\033[0m\n"
print_separator

# Executar o Downloader
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.Downloader "$id" "$input_path" > "$output_path" 2>&1 &
PID=$!

# Imprimir informações do processo
echo -e "\n🚀 Service Started Successfully!"
echo -e "📌 Process ID (PID): \033[1;35m$PID\033[0m"
echo -e "💡 To stop the service, use: \033[1;31mkill $PID\033[0m"
echo -e "📝 Log file: \033[1;33m$output_path\033[0m\n"
print_separator

# Aguardar um momento para verificar se o processo iniciou corretamente
sleep 2
if ps -p $PID > /dev/null; then
    echo -e "✅ Downloader $id is running properly\n"
else
    echo -e "❌ Downloader $id failed to start. Check logs at: $output_path\n"
fi
print_separator
echo ""