#!/bin/bash

# Diretório base do projeto (um nível acima de /scripts)
cd ..

# Criar diretório target se não existir
mkdir -p target

# Definir o classpath com todos os JARs da pasta libs/jars
CLASSPATH=$(find libs/jars/ -name "*.jar" | tr '\n' ':')

# Encontrar todos os arquivos .java em meta1sd e compilar para target
echo "Compilando arquivos Java de meta1sd..."
find meta1sd -name "*.java" -type f | while read -r file; do
    echo "Compilando $file..."
    javac -cp "$CLASSPATH" -d target "$file"
done

echo "Compilação concluída! Arquivos .class foram gerados em target/"