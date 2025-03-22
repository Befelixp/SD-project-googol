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

# Função para validar arquivo de propriedades
validate_properties_file() {
    local file=$1
    if [ ! -f "$file" ]; then
        echo "Error: Properties file $file does not exist!"
        exit 1
    fi
    if [ "$(echo "$file" | grep '\.properties$')" = "" ]; then
        echo "Error: File must have .properties extension!"
        exit 1
    fi
}

# Função para validar ID da barrel
validate_barrel_id() {
    local id=$1
    if ! echo "$id" | grep -E '^[0-9]+$' > /dev/null; then
        echo "Error: Barrel ID must be a positive integer!"
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
        h) echo "Usage: $0 -i barrel_id [-p properties_file] [-o output_file]"
           echo "  -i: Barrel ID (required)"
           echo "  -p: Properties file path (default: config/indexstoragebarrels.properties)"
           echo "  -o: Output log file path"
           echo "  -h: Show this help message"
           exit 0
           ;;
        \?) echo "Invalid option -$OPTARG"
            exit 1
            ;;
    esac
done

# Verificar se o ID da barrel foi fornecido
if [ -z "$barrel_id" ]; then
    echo "Error: Barrel ID is required! Use -i option."
    echo "Usage: $0 -i barrel_id [-p properties_file] [-o output_file]"
    exit 1
fi

# Validar ID da barrel
validate_barrel_id "$barrel_id"

# Validar arquivo de propriedades
validate_properties_file "$input_path"

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$output_path")"

# Informações de execução
echo "Starting Index Storage Barrel with:"
echo "- Barrel ID: $barrel_id"
echo "- Properties file: $input_path"
echo "- Log file: $output_path"
echo "- Classpath: $CLASSPATH"

# Executar o IndexStorageBarrel
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.IndexStorageBarrel "$barrel_id" "$input_path" > "$output_path" 2>&1 &

# Armazenar PID do processo

echo "Index Storage Barrel started with PID: $ibs_pid"