#!/bin/bash

# Diretório base do projeto (um nível acima de /scripts)
cd ..

# Criar diretório target se não existir
mkdir -p target

# Encontrar todos os arquivos .java em src/meta1sd e compilar para target
echo "Compilando arquivos Java de meta1sd..."
find meta1sd -name "*.java" -type f | while read -r file; do
    echo "Compilando $file..."
    javac -d target "$file"
done

echo "Compilação concluída! Arquivos .class foram gerados em target/"