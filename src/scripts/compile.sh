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
echo -e "\n🔨 BUILD PROCESS INITIALIZATION\n"
print_separator

# Diretório base do projeto (um nível acima de /scripts)
cd ..
print_status "info" "Changed to project root directory: \033[1;34m$(pwd)\033[0m"

# Criar diretório target se não existir
if [ ! -d "target" ]; then
    mkdir -p target
    print_status "info" "Created target directory"
fi

# Definir o classpath com todos os JARs da pasta libs/jars
CLASSPATH="."
if [ -d "libs/jars" ]; then
    JARS=$(find libs/jars/ -name "*.jar" | tr '\n' ':')
    CLASSPATH="$CLASSPATH:$JARS"
    print_status "info" "Found JAR files in libs/jars/"
fi

echo -e "\n📚 Classpath Configuration:"
echo -e "\033[0;32m$CLASSPATH\033[0m\n"
print_separator

# Contagem de arquivos Java
JAVA_FILES=$(find meta1sd -name "*.java" -type f | wc -l)
print_status "info" "Found \033[1;36m$JAVA_FILES\033[0m Java files to compile"

# Compilar todos os arquivos .java de uma vez
echo -e "\n🔄 Starting Compilation Process..."
find meta1sd -name "*.java" -type f > sources.txt
print_status "start" "Compiling Java files from meta1sd..."

javac -cp "$CLASSPATH" -d target @sources.txt

if [ $? -eq 0 ]; then
    print_separator
    print_status "success" "Compilation completed successfully!"
    print_status "info" "Class files generated in: \033[1;33mtarget/\033[0m"
    
    # Copiar os arquivos não-Java para target
    echo -e "\n📁 Copying non-Java resources..."
    find meta1sd -type f ! -name "*.java" -exec cp --parents {} target/ \;
    print_status "success" "Resource files copied to target directory"
else
    print_separator
    print_status "error" "Compilation failed!"
    rm sources.txt
    exit 1
fi

# Limpar arquivo temporário
rm sources.txt

# Estatísticas finais
CLASS_FILES=$(find target/meta1sd -name "*.class" -type f | wc -l)
print_separator
echo -e "\n📊 Build Statistics:"
echo -e "  • Java Files Compiled: \033[1;36m$JAVA_FILES\033[0m"
echo -e "  • Class Files Generated: \033[1;36m$CLASS_FILES\033[0m"
echo -e "  • Output Directory: \033[1;33m$(pwd)/target\033[0m\n"
print_separator
echo ""