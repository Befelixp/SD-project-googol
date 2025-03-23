#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Definir variáveis de ambiente e caminhos
JAVA_OPTS="-Xmx512m -Xms256m"
CLASSPATH="$project_root/target:$project_root/libs/jars/*"

# Caminhos padrão
input_path="$project_root/config/indexstoragebarrels.properties"
output_path="$project_root/logs/ibs_$(date +%Y%m%d%H%M%S).log"
barrel_id=""

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

# Função para validar ID da barrel
validate_barrel_id() {
    local id=$1
    if ! echo "$id" | grep -E '^[0-9]+$' > /dev/null; then
        echo "❌ Error: Barrel ID must be a positive integer!"
        exit 1
    fi
}

# Processar argumentos da linha de comando
while getopts ":i:p:o:h" opt; do
    case $opt in
        i) barrel_id="$OPTARG"
           ;;
        p) input_path="$OPTARG"
           ;;
        o) output_path="$OPTARG"
           ;;
        h) echo -e "\n📋 Usage: $0 -i barrel_id [-p properties_file] [-o output_file]"
           echo "  -i: Barrel ID (required)"
           echo "  -p: Properties file path (default: config/indexstoragebarrels.properties)"
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

# Verificar se o ID da barrel foi fornecido
if [ -z "$barrel_id" ]; then
    echo "❌ Error: Barrel ID is required! Use -i option."
    echo "📋 Usage: $0 -i barrel_id [-p properties_file] [-o output_file]"
    exit 1
fi

# Validar ID da barrel
validate_barrel_id "$barrel_id"

# Validar arquivo de propriedades
validate_properties_file "$input_path"

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$output_path")"

# Limpar a tela e mostrar informações de execução
clear
print_separator
echo -e "\n🗄️  INDEX STORAGE BARREL INITIALIZATION\n"
print_separator
echo -e "\n📋 Configuration Details:"
echo -e "  • Barrel ID: \033[1;36m$barrel_id\033[0m"
echo -e "  • Properties File: \033[1;33m$input_path\033[0m"
echo -e "  • Log File: \033[1;33m$output_path\033[0m"
echo -e "  • Classpath: \033[0;32m$CLASSPATH\033[0m\n"
print_separator

# Executar o IndexStorageBarrel
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.IndexStorageBarrel "$barrel_id" "$input_path" > "$output_path" 2>&1 &
PID=$!

# Imprimir informações do processo
echo -e "\n🚀 Service Started Successfully!"
echo -e "📌 Process ID (PID): \033[1;35m$PID\033[0m"
echo -e "💡 To stop the service, use: \033[1;31mkill $PID\033[0m\n"
print_separator
echo ""