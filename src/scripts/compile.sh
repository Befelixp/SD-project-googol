#!/bin/bash

# Diretório base do projeto (um nível acima de /scripts)
cd ..

# Criar diretório target se não existir
mkdir -p target

# Definir o classpath com todos os JARs da pasta libs/jars
CLASSPATH="."
if [ -d "libs/jars" ]; then
    JARS=$(find libs/jars/ -name "*.jar" | tr '\n' ':')
    CLASSPATH="$CLASSPATH:$JARS"
fi

echo "Usando CLASSPATH: $CLASSPATH"

# Compilar todos os arquivos .java de uma vez
echo "Compilando arquivos Java de meta1sd..."
find meta1sd -name "*.java" -type f > sources.txt
javac -cp "$CLASSPATH" -d target @sources.txt

if [ $? -eq 0 ]; then
    echo "Compilação concluída com sucesso! Arquivos .class foram gerados em target/"
    # Copiar os arquivos não-Java para target também
    find meta1sd -type f ! -name "*.java" -exec cp --parents {} target/ \;
else
    echo "Erro durante a compilação!"
    exit 1
fi

# Limpar arquivo temporário
rm sources.txt