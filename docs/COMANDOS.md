# Comandos Manuais para Execução do Projeto

Este documento explica como executar e parar os componentes do projeto manualmente, sem usar os scripts PowerShell.

## Compilação

Para compilar o projeto manualmente:

```bash
# Compilar todos os arquivos Java com as dependências
javac -cp "meta1sd/src/libs/jars/*" meta1sd/src/meta1sd/*.java

# Criar o arquivo JAR
jar cf meta1sd.jar -C meta1sd/src .
```

## Execução dos Componentes

### Gateway
```bash
# Iniciar o Gateway
java -cp "meta1sd.jar;meta1sd/src/libs/jars/*" meta1sd.RMIGateway config/gateway.properties
```

### Index Storage Barrel (IBS)
```bash
# Iniciar um Barrel (substitua <ID> pelo número do barrel, ex: 1, 2, etc)
java -cp "meta1sd.jar;meta1sd/src/libs/jars/*" meta1sd.IndexStorageBarrel <ID> config/indexstoragebarrels.properties
```

### Downloader
```bash
# Iniciar o Downloader
java -cp "meta1sd.jar;meta1sd/src/libs/jars/*" meta1sd.Downloader config/downloaders.properties
```

### Cliente
```bash
# Iniciar um Cliente (substitua <ID> pelo ID do cliente)
java -cp "meta1sd.jar;meta1sd/src/libs/jars/*" meta1sd.RMIClient <ID> config/client.properties
```

## Parar os Componentes

Para parar os componentes manualmente, você pode usar os seguintes métodos:

### Método 1: Usando o PowerShell
```powershell
# Listar todos os processos Java
Get-Process java

# Parar um processo específico (substitua <PID> pelo ID do processo)
Stop-Process -Id <PID>

# Parar todos os processos Java (use com cuidado!)
Get-Process java | Stop-Process
```

### Método 2: Usando o Command Prompt (cmd)
```cmd
# Listar todos os processos Java
tasklist | findstr "java"

# Parar um processo específico (substitua <PID> pelo ID do processo)
taskkill /PID <PID>

# Parar todos os processos Java (use com cuidado!)
taskkill /F /IM java.exe
```

### Método 3: Manualmente
1. Pressione `Ctrl+C` no terminal onde o componente está rodando
2. Confirme a interrupção se solicitado
