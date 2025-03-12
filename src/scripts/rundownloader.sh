#!/bin/bash

# Caminho absoluto para o diretório raiz do projeto
project_root=$(dirname $(dirname $(realpath $0)))

# Definir variáveis de ambiente e caminhos
JAVA_OPTS="-Xmx512m -Xms256m"
CLASSPATH="$project_root/target:$project_root/libs/jars/*"

# Caminhos padrão
input_path="$project_root/config/downloaders.properties"
output_path="$project_root/logs/downloader_$(date +%Y%m%d%H%M%S).log"

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


# Processar argumentos da linha de comando
while getopts ":i:p:o:h" opt; do
    case $opt in
        i) id="$OPTARG"
           ;;
        p) input_path="$OPTARG"
           ;;
        o) output_path="$OPTARG"
           ;;
        h) echo "Usage: $0 [-i id] [-p properties_file] [-o output_file]"
           exit 0
           ;;
        \?) echo "Invalid option -$OPTARG"
            exit 1
            ;;
    esac
done

# Verificar se o ID foi fornecido
if [ -z "$id" ]; then
    # Se não foi fornecido, usar ID padrão 1
    id="1"
fi

# Validar arquivo de propriedades
validate_properties_file "$input_path"

# Criar diretório de logs se não existir
mkdir -p "$(dirname "$output_path")"

# Informações de execução
echo "Starting Downloader with:"
echo "- ID: $id"
echo "- Properties file: $input_path"
echo "- Log file: $output_path"
echo "- Classpath: $CLASSPATH"

# Executar o Downloader
cd "$project_root"
java $JAVA_OPTS -cp "$CLASSPATH" meta1sd.Downloader "$id" "$input_path" > "$output_path" 2>&1 &

